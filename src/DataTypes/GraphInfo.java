package DataTypes;
import java.util.*;
import NetworkElements.*;

/**
 * GraphInfo - graph information
 * @author Zhengyang Zuo
 *
 */
public class GraphInfo {
	static public HashMap<Integer, ArrayList<Integer>> graph = new HashMap<Integer, ArrayList<Integer>>();
	static public HashMap<Integer, ArrayList<LSRNIC>> nics = new HashMap<Integer, ArrayList<LSRNIC>>();
}