package NetworkElements;

import java.util.*;

import DataTypes.*;

public class LSR{
	private int address; // The AS address of this router
	private ArrayList<LSRNIC> nics = new ArrayList<LSRNIC>(); // all of the nics in this router
	private TreeMap<Integer, LSRNIC> nextHop = new TreeMap<Integer, LSRNIC>(); // a map of which interface to use to get to a given router on the network
	private TreeMap<Integer, NICLabelPair> LabeltoLabel = new TreeMap<Integer, NICLabelPair>(); // a map of input VC to output nic and new VC number
	private HashMap<DestDSCPPair, Integer> destDSCPtoLabel = new HashMap<DestDSCPPair, Integer>();
	private HashMap<DestDSCPPair, Integer> destDSCPtoBW = new HashMap<DestDSCPPair, Integer>();	// map between traffic class and its allocated bandwidth
	private ArrayList<Packet> waitList = new ArrayList<Packet>();	// packets waiting to be send due to path setting up
	
	private boolean trace = false; // should we print out debug code?
	private int traceID = (int) (Math.random() * 100000); // create a random trace id for cells
	private LSRNIC currentConnAttemptNIC = null; // The nic that is currently trying to setup a connection
	private boolean displayCommands = true; // should we output the commands that are received?
	private boolean isStart = true;	// used for deciding if setting up nexthop table
	
	private int remainBandwidth = 50;	// for bandwidth reservation
	
	/**
	 * The default constructor for an ATM router
	 * @param address the address of the router
	 * @since 1.0
	 */
	public LSR(int address){
		this.address = address;
		GraphInfo.graph.put(this.address, new ArrayList<Integer>());
		GraphInfo.nics.put(this.address, new ArrayList<LSRNIC>());
	}
	
	/**
	 * The return the router's address
	 * @since 1.0
	 */
	public int getAddress(){
		return this.address;
	}
	
	/**
	 * Adds a nic to this router
	 * @param nic the nic to be added
	 * @since 1.0
	 */
	public void addNIC(LSRNIC nic){
		this.nics.add(nic);
	}
	
	/**
	 * This method processes data and OAM cells that arrive from any nic with this router as a destination
	 * @param currentPacket the packet that arrived at this router
	 * @param nic the nic that the cell arrived on
	 * @since 1.0
	 */
	public void receivePacket(Packet currentPacket, LSRNIC nic){
		if (this.isStart) {
			this.calculateNextHop();
			this.isStart = false;
		}

		if(currentPacket.getIsOAM()){	// OAM packet
			int toAddress = currentPacket.getDest();
			
			// PATH
			if (currentPacket.getIsPath()) {					
				
				if (this.address == toAddress) {	// dest address match
					this.receivedPath(currentPacket);
					
					// send RESV
					int thisLabel;
					if (!this.LabeltoLabel.isEmpty()) {
						thisLabel = this.LabeltoLabel.lastKey() + 1;
					}
					else {
						thisLabel = 1;
					}
					if (trace) {
						System.out.println("Trace (ATMRouter): First free LSP = " + thisLabel);
					}
					this.LabeltoLabel.put(thisLabel, new NICLabelPair(nic, thisLabel));
					
					Packet resv = new Packet(this.getAddress(), currentPacket.getSource(), currentPacket.getDSCP());
					resv.setIsOAM(true);
					resv.setIsResv(true);
					resv.addMPLSheader(new MPLS(thisLabel, 0, 1));
					resv.setTraceID(this.getTraceID());
					resv.setBandwidth(currentPacket.getBandwidth());
					this.sentResv(resv);
					nic.sendPacket(resv, this);					
				}
				
				else {	// not dest address
					if (this.nics.size() <= 1) {	// invalid end point
						System.out.println("Nowhere to forward");
						return;
					}						
					currentConnAttemptNIC = nic;						
					this.receivedPath(currentPacket);				
					if (this.nextHop.containsKey(toAddress)) {
						LSRNIC nicSent = this.nextHop.get(toAddress);
						this.sentPath(currentPacket);
						nicSent.sendPacket(currentPacket, this);
						//this.currentConnAttemptNIC = nicSent;
					}
					else {	// can't find dest addr in nexthop, send PATHERR
						Packet patherr = new Packet(currentPacket.getDest(), currentPacket.getSource(), currentPacket.getDSCP());
						patherr.setIsOAM(true);
						patherr.setIsPathErr(true);
						this.sentPathErr(patherr);
						nic.sendPacket(patherr, this);
					}
				}
			}
			
			// PATHERR
			else if (currentPacket.getIsPathErr()) {	
				this.receivedPathErr(currentPacket);
				if (currentPacket.getDest() == this.getAddress()) {	// resent PATH
					Packet resent = new Packet(currentPacket.getDest(), currentPacket.getSource(), currentPacket.getDSCP());
					resent.setIsOAM(true);
					resent.setIsPath(true);
					resent.setTraceID(this.getTraceID());
					this.sentPath(resent);
					nic.sendPacket(resent, this);
				}
				else {	// forward PATHERR
					this.sentPathErr(currentPacket);
					this.currentConnAttemptNIC.sendPacket(currentPacket, this);
				}
			}
			
			// RESVERR
			else if (currentPacket.getIsPathErr()) {	
				this.receivedPathErr(currentPacket);
				if (currentPacket.getDest() == this.getAddress()) {	// resent RESV
					Packet resent = new Packet(currentPacket.getDest(), currentPacket.getSource(), currentPacket.getDSCP());
					resent.setIsOAM(true);
					resent.setIsResv(true);
					resent.setTraceID(this.getTraceID());
					resent.setBandwidth(currentPacket.getBandwidth());
					resent.addMPLSheader(currentPacket.getFirstMPLS());   // ????????????????
					this.sentResv(resent);
					nic.sendPacket(resent, this);
				}
				else {	// forward RESVERR
					this.sentResvErr(currentPacket);
					LSRNIC fwdnic = this.nextHop.get(currentPacket.getSource());
					fwdnic.sendPacket(currentPacket, this);
				}
			}
			
			// RESV
			else if (currentPacket.getIsResv()) {
				int inLabel = currentPacket.getFirstMPLS().getLabel();
				int outLabel = 1;
				this.receivedResv(currentPacket);
				
				if (!this.LabeltoLabel.isEmpty()) {
					outLabel = this.LabeltoLabel.lastKey() + 1;
					for (int i = 1; i < this.LabeltoLabel.lastKey(); i ++) {
						if (!this.LabeltoLabel.containsKey(i)) {
							outLabel = i;
							break;
						}
					}
				}
				
				// Allocate Bandwidth
				if (this.remainBandwidth >= currentPacket.getBandwidth()) {
					this.remainBandwidth -= currentPacket.getBandwidth();
				}
				else {	// unable to reserve, send RESVERR
					Packet resverr = new Packet(currentPacket.getDest(), currentPacket.getSource(), currentPacket.getDSCP());
					resverr.setIsOAM(true);
					resverr.setIsResvErr(true);
					resverr.setTraceID(this.getTraceID());
					resverr.setBandwidth(currentPacket.getBandwidth());
					resverr.addMPLSheader(currentPacket.getFirstMPLS());	//????????????????
					this.sentResvErr(resverr);
					nic.sendPacket(resverr, this);
					return;
				}
				
				// forward RESV
				if (currentPacket.getDest() != this.getAddress()) {
					Packet resv = new Packet(currentPacket.getSource(), currentPacket.getDest(), currentPacket.getDSCP());
					resv.setIsOAM(true);
					resv.setIsResv(true);
					resv.setTraceID(this.getTraceID());
					resv.addMPLSheader(currentPacket.getFirstMPLS());
					resv.getFirstMPLS().setLabel(outLabel);
					this.sentResv(resv);
					LSRNIC fwdnic = this.nextHop.get(currentPacket.getDest());
					fwdnic.sendPacket(resv, this);
					this.LabeltoLabel.put(outLabel, new NICLabelPair(nic, inLabel));
					this.currentConnAttemptNIC = null;
				}
				else {	// RESV reaches the SOURCE node
					this.destDSCPtoLabel.put(new DestDSCPPair(currentPacket.getSource(), currentPacket.getDSCP()), outLabel);
					this.LabeltoLabel.put(outLabel, new NICLabelPair(nic, inLabel));
					if (trace) {
						System.out.println("The connection is setup on LSP " + outLabel);
					}
					
					// send RESVCONF
					Packet conf = new Packet(currentPacket.getDest(), currentPacket.getSource(), currentPacket.getDSCP());
					conf.setIsOAM(true);
					conf.setIsResvConf(true);
					conf.setTraceID(this.getTraceID());
					this.sentResvConf(conf);
					nic.sendPacket(conf, this);
														
					// send packets in the waiting list
					ArrayList<Integer> deleteID = new ArrayList<Integer>();
					for (int i = 0; i < this.waitList.size(); i ++) {
						Packet packet = this.waitList.get(i);
						if (this.destDSCPtoLabel.containsKey(new DestDSCPPair(packet.getDest(), packet.getDSCP())) 
								&& this.destDSCPtoLabel.get(new DestDSCPPair(packet.getDest(), packet.getDSCP())) != -1) {
							packet.addMPLSheader(new MPLS(inLabel, 0, 1));
							nic.sendPacket(packet, this);
							if (trace) {
								System.out.println("Sending packet " + packet.getTraceID() + " from router " + this.getAddress());
							}
							deleteID.add(i);
						}
					}
					for (int i = deleteID.size() - 1; i >= 0; i --) {
						int tmp = deleteID.get(i);
						this.waitList.remove(tmp);
					}					
					return;
				}
			}
			
			// RESVCONF
			else if (currentPacket.getIsResvConf()) {
				this.receivedResvConf(currentPacket);
				if (this.getAddress() != currentPacket.getDest()) {
					LSRNIC fwdnic = this.nextHop.get(currentPacket.getDest());
					fwdnic.sendPacket(currentPacket, this);
				}
			}
			
			else {
				System.out.println("Error: Message not implemented.");
			}			
		}
		
		// send NORMAL packets
		else {	
			// find the nic and new LSP number to forward the packet on
			// otherwise the packet has nowhere to go. output to the console and drop the packet
			if (this.LabeltoLabel.isEmpty()) {
				System.out.println("Error: vc lookup table is empty.");
				return;
			}
			if (!this.LabeltoLabel.containsKey(currentPacket.getFirstMPLS().getLabel())) {
				System.out.println("Error: No VC found.");
				return;
			}
			int outLabel = this.LabeltoLabel.get(currentPacket.getFirstMPLS().getLabel()).getVC();
			LSRNIC outNIC = this.LabeltoLabel.get(currentPacket.getFirstMPLS().getLabel()).getNIC();
			if (outNIC != nic) {
				currentPacket.getFirstMPLS().setLabel(outLabel);
				outNIC.sendPacket(currentPacket, this);
				if (this.trace) {
					System.out.println("Sending packet " + currentPacket.getTraceID() + " from router " + this.getAddress() + " to " + currentPacket.getDest());
				}
			}
			else {
				if (trace) {
					System.out.println("Packet " + currentPacket.getTraceID() + " reaches the end at " + this.getAddress());
				}
			}
			
		}	
	}
	
	/**
	 * This method creates a packet with the specified type of service field and sends it to a destination
	 * @param destination the distination router
	 * @param DSCP the differentiated services code point field
	 * @since 1.0
	 */
	public void createPacket(int destination, int DSCP) {
		if (this.isStart) {
			this.calculateNextHop();
			this.isStart = false;
		}
		Packet newPacket= new Packet(this.getAddress(), destination, DSCP);
		newPacket.setTraceID(this.getTraceID());
		this.sendPacket(newPacket);				
	}

	/**
	 * This method allocates bandwidth for a specific traffic class from the current router to the destination router
	 * @param dest destination router id
	 * @param PHB 0=EF, 1=AF, 2=BE
	 * @param Class AF classes 1,2,3,4. (0 if EF or BE)
	 * @param Bandwidth number of packets per time unit for this PHB/Class
	 * @since 1.0
	 */
	public void allocateBandwidth(int dest, int PHB, int Class, int Bandwidth) {
		int DSCP = 0;
		if (PHB == 1) {
			DSCP = Class;
		}
		if (PHB == 2) {
			DSCP = 5;
		}
		this.destDSCPtoBW.put(new DestDSCPPair(dest, DSCP), Bandwidth);
	}
	
	/**
	 * This method forwards a packet to the correct nic or drops if at destination router
	 * @param newPacket The packet that has just arrived at the router.
	 * @since 1.0
	 */
	public void sendPacket(Packet newPacket) {
		
		//This method should send the packet to the correct NIC.]
		int dest = newPacket.getDest();
		int DSCP = newPacket.getDSCP();
		DestDSCPPair pair = new DestDSCPPair(dest, DSCP);
		LSRNIC nic = this.nextHop.get(dest);
		
		if (this.destDSCPtoLabel.containsKey(pair) && this.destDSCPtoLabel.get(pair) != -1) {
			int inLabel = this.destDSCPtoLabel.get(pair);
			int outLabel = this.LabeltoLabel.get(inLabel).getVC();
			newPacket.addMPLSheader(new MPLS(outLabel, 0, 1));
			nic.sendPacket(newPacket, this);
			if (this.trace) {
				System.out.println("Sending packet " + newPacket.getTraceID() + " from router " + this.getAddress());
			}
		}
		
		else if (!this.destDSCPtoLabel.containsKey(pair)) {
			this.destDSCPtoLabel.put(pair, -1);
			this.waitList.add(newPacket);
			Packet path = new Packet(this.getAddress(), newPacket.getDest(), newPacket.getDSCP());
			path.setIsOAM(true);
			path.setIsPath(true);
			path.setTraceID(this.getTraceID());
			if (this.destDSCPtoBW.containsKey(new DestDSCPPair(newPacket.getDest(), newPacket.getDSCP()))) {
				path.setBandwidth(this.destDSCPtoBW.get(new DestDSCPPair(newPacket.getDest(), newPacket.getDSCP())));
			}
			this.sentPath(path);
			nic.sendPacket(path, this);
		}		
		else {
			this.waitList.add(newPacket);
		}
		
	}

	/**
	 * Makes each nic move its cells from the output buffer across the link to the next router's nic
	 * @since 1.0
	 */
	public void sendPackets(){
		for(int i=0; i<this.nics.size(); i++)
			this.nics.get(i).sendPackets();
	}
	
	/**
	 * Makes each nic move all of its cells from the input buffer to the output buffer
	 * @since 1.0
	 */
	public void recievePackets(){
		for(int i=0; i<this.nics.size(); i++)
			this.nics.get(i).recievePackets();
	}
	
	/**
	 * Tells the router the nic to use to get towards a given router on the network
	 * @param destAddress the destination address of the ATM router
	 * @param outInterface the interface to use to connect to that router
	 * @since 1.0
	 */
	public void addNextHopInterface(int destAddress, LSRNIC outInterface){
		this.nextHop.put(destAddress, outInterface);
	}
	
	/**
	 * Using Dijkstra algorithm to calculate this.nexthop
	 * @since 1.0
	 */
	public void calculateNextHop(){
		int origin = this.getAddress();
		int v0 = 0;		// the origin node
		ArrayList<Integer> nodes = new ArrayList<Integer>();
		
		for (Integer node : GraphInfo.graph.keySet()) {
			nodes.add(node);
			if (node == origin) {
				v0 = nodes.size() - 1;
			}		
		}
		
		int n = nodes.size();	
		ArrayList<Integer> dist = new ArrayList<Integer>();	// distance to origin node
		ArrayList<Integer> path = new ArrayList<Integer>();	// path to origin node
		ArrayList<Boolean> visited = new ArrayList<Boolean>();
		ArrayList<LSRNIC> niclist = GraphInfo.nics.get(origin);
		ArrayList<Integer> neighborlist = GraphInfo.graph.get(origin);
		
		for (int i = 0; i < n; i ++) {
			dist.add(0);
			path.add(-1);
			visited.add(false);
		}
		for (int i = 0; i < n; i ++) {
			if (neighborlist.contains(nodes.get(i)) 
					&& nodes.get(i) != origin) {
				dist.set(i, 1);
				path.set(i, v0);
			}
			else {
				dist.set(i, Integer.MAX_VALUE);
				path.set(i, -1);
			}
			visited.set(i, false);
			path.set(v0, v0);
			dist.set(v0, 0);
		}
		visited.set(v0, true);
		for (int i = 1; i < n; i ++) {
			int min = Integer.MAX_VALUE;
			int u = 0;
			for (int j = 0; j < n; j ++) {
				if (visited.get(j) == false && dist.get(j) < min) {
					min = dist.get(j);
					u = j;
				}
			}
			visited.set(u, true);
			for (int k = 0; k < n; k ++) {
				if (visited.get(k) == false 
						&& GraphInfo.graph.get(nodes.get(u)).contains(nodes.get(k))
						&& min + 1 < dist.get(k)) {
					dist.set(k, min + 1);
					path.set(k, u);
				}
			}
		}
		
		for (int i = 0; i < n; i ++) {
			if (i == v0) {
				continue;
			}
			int tmp = i;
			while (path.get(tmp) != v0) {
				tmp = path.get(tmp);
			}
			int id;
			for (id = 0; id < neighborlist.size(); id ++) {
				if (neighborlist.get(id) == nodes.get(tmp)) {
					break;
				}
			}
			
			this.nextHop.put(nodes.get(i), niclist.get(id));
		}

		return;
	}
	
	
	
	
	/**
	 * This method returns a sequentially increasing random trace ID, so that we can
	 * differentiate cells in the network
	 * @return the trace id for the next cell
	 * @since 1.0
	 */
	public int getTraceID(){
		int ret = this.traceID;
		this.traceID++;
		return ret;
	}
	
	/**
	 * Outputs to the console that a PATH message has been sent
	 * @since 1.0
	 */
	private void sentPath(Packet packet){
		if(this.displayCommands)
		System.out.println("Router " +this.address+ " sent a PATH to Router " + packet.getDest());
	}
	
	/**
	 * Outputs to the console that a PATH message has been received
	 * @since 1.0
	 */
	private void receivedPath(Packet packet){
		if(this.displayCommands)
		System.out.println("Router " +this.address+ " received a PATH from Router " + packet.getSource());
	}
	
	/**
	 * Outputs to the console that a RESV message has been sent
	 * @since 1.0
	 */
	private void sentResv(Packet packet){
		if(this.displayCommands)
		System.out.println("Router " +this.address+ " sent a RESV to Router " + packet.getDest());
	}
	
	/**
	 * Outputs to the console that a RESV message has been received
	 * @since 1.0
	 */
	private void receivedResv(Packet packet){
		if(this.displayCommands)
		System.out.println("Router " +this.address+ " received a RESV from Router " + packet.getSource());
	}
	
	/**
	 * Outputs to the console that a PATHERR message has been sent
	 * @since 1.0
	 */
	private void sentPathErr(Packet packet){
		if(this.displayCommands)
		System.out.println("Router " +this.address+ " sent a PATHERR to Router " + packet.getDest());
	}
	
	/**
	 * Outputs to the console that a PATHERR message has been received
	 * @since 1.0
	 */
	private void receivedPathErr(Packet packet){
		if(this.displayCommands)
		System.out.println("Router " +this.address+ " received a PATHERR from Router " + packet.getSource());
	}
	
	/**
	 * Outputs to the console that a RESVERR message has been sent
	 * @since 1.0
	 */
	private void sentResvErr(Packet packet){
		if(this.displayCommands)
		System.out.println("Router " +this.address+ " sent a RESVERR to Router " + packet.getDest());
	}
	
	/**
	 * Outputs to the console that a RESVERR message has been received
	 * @since 1.0
	 */
	private void receivedResvErr(Packet packet){
		if(this.displayCommands)
		System.out.println("Router " +this.address+ " received a RESVERR from Router " + packet.getSource());
	}
	
	/**
	 * Outputs to the console that a RESVCONF message has been sent
	 * @since 1.0
	 */
	private void sentResvConf(Packet packet){
		if(this.displayCommands)
		System.out.println("Router " +this.address+ " sent a RESVCONF to Router " + packet.getDest());
	}
	
	/**
	 * Outputs to the console that a RESVCONF message has been received
	 * @since 1.0
	 */
	private void receivedResvConf(Packet packet){
		if(this.displayCommands)
		System.out.println("Router " +this.address+ " received a RESVCONF from Router " + packet.getSource());
	}
	
}
