package DataTypes;

import java.util.LinkedList;
import java.util.Queue;

public class Packet {
	private int source, dest, DSCP; // The source and destination addresses
	private boolean OAM = false;
	private Queue<MPLS> MPLSheader = new LinkedList<MPLS>(); // all of the MPLS headers in this router
	private boolean isPath = false;
	private boolean isResv = false;
	private boolean isResvConf = false;
	private boolean isPathErr = false;
	private boolean isResvErr = false;
	private boolean isWait = false;
	private int traceID = 0;	// The trace ID for the packet

	
	/**
	 * The default constructor for a packet
	 * @param source the source ip address of this packet
	 * @param dest the destination ip address of this packet
	 * @param DSCP Differential Services Code Point
	 * @since 1.0
	 */
	public Packet(int source, int dest, int DSCP){
		try{
			this.source = source;
			this.dest = dest;
			this.DSCP = DSCP;
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Adds an MPLS header to a packet
	 * @since 1.0
	 */
	public void addMPLSheader(MPLS header){
		MPLSheader.add(header);
	}
	
	/**
	 * Pops an MPLS header from the packet
	 * @since 1.0
	 */
	public MPLS popMPLSheader(){
		return MPLSheader.poll();
	}
	
	/**
	 * Returns the source ip address of this packet
	 * @return the source ip address of this packet
	 * @since 1.0
	 */
	public int getSource(){
		return this.source;
	}
	
	/**
	 * Returns the destination ip address of this packet
	 * @return the destination ip address of this packet
	 * @since 1.0
	 */
	public int getDest(){
		return this.dest;
	}

	/**
	 * Set the DSCP field
	 * @param DSCP the DSCP field value
	 * @since 1.0
	 */
	public void setDSCP(int dSCP) {
		this.DSCP = dSCP;
	}

	/**
	 * Returns the DSCP field
	 * @return the DSCP field
	 * @since 1.0
	 */
	public int getDSCP() {
		return this.DSCP;
	}
	
	/**
	 * Set the OAM 
	 * 
	 */
	public void setIsOAM(boolean flag) {
		this.OAM = flag;
	}
	
	/**
	 * Get the OAM 
	 * 
	 */
	public boolean getIsOAM() {
		return this.OAM;
	}
	
	/**
	 * Set isPath
	 */
	public void setIsPath(boolean flag) {
		this.isPath = flag;
	}
	
	/**
	 * Get isPath
	 */
	public boolean getIsPath() {
		return this.isPath;
	}
	
	/**
	 * Set isResv
	 */
	public void setIsResv(boolean flag) {
		this.isResv = flag;
	}
	
	/**
	 * Get isResv
	 */
	public boolean getIsResv() {
		return this.isResv;
	}
	
	/**
	 * Set isResvConf
	 */
	public void setIsResvConf(boolean flag) {
		this.isResvConf = flag;
	}
	
	/**
	 * Get isResvConf
	 */
	public boolean getIsResvConf() {
		return this.isResvConf;
	}
	
	/**
	 * Set isPathErr
	 */
	public void setIsPathErr(boolean flag) {
		this.isPathErr = flag;
	}
	
	/**
	 * Get isPathErr
	 */
	public boolean getIsPathErr() {
		return this.isPathErr;
	}
	
	/**
	 * Set isResvErr
	 */
	public void setIsResvErr(boolean flag) {
		this.isResvErr = flag;
	}
	
	/**
	 * Get isWait
	 */
	public boolean getIsWait() {
		return this.isWait;
	}
	
	/**
	 * Set isWait
	 */
	public void setIsWait(boolean flag) {
		this.isWait = flag;
	}
	
	/**
	 * Get isResvErr
	 */
	public boolean getIsResvErr() {
		return this.isResvErr;
	}
	
	/**
	 * Returns the trace ID for this cell
	 * @return the trace ID for this cell
	 * @since 1.0
	 */
	public int getTraceID(){
		return this.traceID;
	}
	
	public void setTraceID(int id) {
		this.traceID = id;
	}
	
	/**
	 * Get addr for wait
	 */
	/*public int getAddrForWait() {
		return this.addrForWait;
	}*/
	
	/**
	 * Set addr for wait
	 */
	/*public void setAddrForWait(int addr) {
		this.addrForWait = addr;
	}*/
	
	/**
	 * Get first MPLS header
	 * @return first MPLS header
	 */
	public MPLS getFirstMPLS() {
		return this.MPLSheader.peek();
	}
}
	
