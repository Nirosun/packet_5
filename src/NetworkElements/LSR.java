package NetworkElements;

import java.util.*;

import DataTypes.*;

public class LSR{
	private int address; // The AS address of this router
	private ArrayList<LSRNIC> nics = new ArrayList<LSRNIC>(); // all of the nics in this router
	private TreeMap<Integer, LSRNIC> nextHop = new TreeMap<Integer, LSRNIC>(); // a map of which interface to use to get to a given router on the network
	private TreeMap<Integer, NICVCPair> VCtoVC = new TreeMap<Integer, NICVCPair>(); // a map of input VC to output nic and new VC number
	private boolean trace=true; // should we print out debug code?
	private int traceID = (int) (Math.random() * 100000); // create a random trace id for cells
	private LSRNIC currentConnAttemptNIC = null; // The nic that is currently trying to setup a connection
	private boolean displayCommands = true; // should we output the commands that are received?
	private boolean isStart = true;
	
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
		
	}
	
	/**
	 * This method creates a packet with the specified type of service field and sends it to a destination
	 * @param destination the distination router
	 * @param DSCP the differentiated services code point field
	 * @since 1.0
	 */
	public void createPacket(int destination, int DSCP){
		Packet newPacket= new Packet(this.getAddress(), destination, DSCP);
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
	public void allocateBandwidth(int dest, int PHB, int Class, int Bandwidth){
		
	}
	
	/**
	 * This method forwards a packet to the correct nic or drops if at destination router
	 * @param newPacket The packet that has just arrived at the router.
	 * @since 1.0
	 */
	public void sendPacket(Packet newPacket) {
		
		//This method should send the packet to the correct NIC.
		
	}

	/**
	 * Makes each nic move its cells from the output buffer across the link to the next router's nic
	 * @since 1.0
	 */
	public void sendPackets(){
		if (this.isStart) {
			this.calculateNextHop();
			this.isStart = false;
		}
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
	 * Calculate nexthop
	 * @param destAddress the destination address of the ATM router
	 * @param outInterface the interface to use to connect to that router
	 * @since 1.0
	 */
	public void calculateNextHop(){
		//this.nextHop.put(destAddress, outInterface);
		int origin = this.getAddress();
		int v0 = 0;
		//Set<Integer> Vset = GraphInfo.graph.keySet();
		ArrayList<Integer> nodes = new ArrayList<Integer>();
		
		for (Integer node : GraphInfo.graph.keySet()) {
			nodes.add(node);
			if (node == origin) {
				v0 = nodes.size() - 1;
			}		
		}
		
		int n = nodes.size();
		ArrayList<Integer> dist = new ArrayList<Integer>();
		ArrayList<Integer> path = new ArrayList<Integer>();
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

		v0=v0+1;
		return;
	}
}
