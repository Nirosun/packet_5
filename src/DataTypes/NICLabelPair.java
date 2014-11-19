/**
 * @author andy
 * @version 1.0
 * @date 24-10-2008
 * @since 1.0
 */

package DataTypes;

import NetworkElements.*;

public class NICLabelPair implements Comparable<NICLabelPair>{
	private LSRNIC nic; // The nic of the pair
	private int label; // the VC of the pair
	
	/**
	 * Constructor for a pair of (nic, vc)
	 * @param nic the nic that is in the pair
	 * @param vc the vc that is in the pair
	 * @since 1.0
	 */
	public NICLabelPair(LSRNIC nic, int label){
		this.nic = nic;
		this.label = label;
	}
	
	/**
	 * Returns the nic that makes up half of the pair
	 * @return the nic that makes up half of the pair
	 * @since 1.0
	 */
	public LSRNIC getNIC(){
		return this.nic;
	}
	
	/**
	 * Returns the nic that makes up half of the pair
	 * @return the nic that makes up half of the pair
	 * @since 1.0
	 */
	public int getVC(){
		return this.label;
	}
	
	/**
	 * Returns whether or not a given object is the same as this pair. I.e. it is a pair containing the same nic and vc
	 * @return true/false the given object of the same as this object
	 * @since 1.0
	 */
	public boolean equals(Object o){
		if(o instanceof NICLabelPair){
			NICLabelPair other = (NICLabelPair) o;
			
			if(other.getNIC()==this.getNIC() && other.getVC()==this.getVC())
				return true;
		}
		
		return false;
	}
	
	/**
	 * Allows this object to be used in a TreeMap
	 * @returns if this object is less than, equal to, or greater than a given object
	 * @since 1.0
	 */
	public int compareTo(NICLabelPair o){
		return this.getVC()-o.getVC();
	}
}
