package DataTypes;

public class MPLS {
	private int label; // the MPLS label
	private int trafficclass; // the DiffServ traffic class
	private int stackingbit=1; // 1 if last header. 0 otherwise
	
	/**
	 * The default constructor for a MPLS header
	 * @param label the MPLS label
	 * @param trafficclass the DiffServ traffic class
	 * @param stackingbit 1 if last header. 0 otherwise
	 * @since 1.0
	 */
	public MPLS(int label, int trafficclass, int stackingbit){
		try{
			this.label = label;
			this.trafficclass = trafficclass;
			this.stackingbit = stackingbit;
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns the label of this MPLS header
	 * @return the label of this MPLS header
	 * @since 1.0
	 */
	public int getLabel(){
		return this.label;
	}
	
	/**
	 * Returns the traffic class of this MPLS header
	 * @return the traffic class of this MPLS header
	 * @since 1.0
	 */
	public int getTrafficClass(){
		return this.trafficclass;
	}
	
	/**
	 * Returns the stacking bit of this MPLS header
	 * @return the stacking bit of this MPLS header
	 * @since 1.0
	 */
	public int getStackingBit(){
		return this.stackingbit;
	}
}