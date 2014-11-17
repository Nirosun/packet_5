package DataTypes;

import java.util.LinkedList;
import java.util.Queue;

public class Packet {
	private int source, dest, DSCP; // The source and destination addresses
	private boolean OAM = false;
	private Queue<MPLS> MPLSheader = new LinkedList<MPLS>(); // all of the MPLS headers in this router
	
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
}
	
