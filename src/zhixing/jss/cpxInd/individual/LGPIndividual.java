package zhixing.jss.cpxInd.individual;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Vector;

import javax.lang.model.element.ExecutableElement;

import java.util.Iterator;

import org.apache.commons.math3.util.Pair;
import org.spiderland.Psh.booleanStack;
import org.spiderland.Psh.intStack;

//import com.sun.xml.internal.bind.v2.runtime.output.C14nXmlOutput;

import ec.EvolutionState;
import ec.Problem;
import ec.app.tutorial4.MultiValuedRegression;
import ec.gp.*;
import ec.util.Code;
import ec.util.Output;
import ec.util.Parameter;
import zhixing.symbolicregression.optimization.LGPSymbolicRegression;
import yimei.jss.gp.CalcPriorityProblem;
import yimei.jss.gp.GPRuleEvolutionState;
import yimei.jss.gp.data.DoubleData;
import yimei.jss.gp.terminal.AttributeGPNode;
import yimei.jss.gp.terminal.JobShopAttribute;
import yimei.jss.gp.terminal.TerminalERC;
import yimei.jss.jobshop.Objective;
import yimei.jss.jobshop.Operation;
import yimei.jss.jobshop.SchedulingSet;
import yimei.jss.jobshop.WorkCenter;
import yimei.jss.rule.AbstractRule;
import yimei.jss.simulation.DecisionSituation;
import yimei.jss.simulation.Simulation;
import yimei.jss.simulation.state.SystemState;
import yimei.util.lisp.LispParser;
import zhixing.jss.cpxInd.individual.primitive.Branching;
import zhixing.jss.cpxInd.individual.primitive.FlowOperator;
import zhixing.jss.cpxInd.individual.primitive.IFLargerThan;
import zhixing.jss.cpxInd.individual.primitive.Iteration;
import zhixing.jss.cpxInd.individual.primitive.ReadConstantRegisterGPNode;
import zhixing.jss.cpxInd.individual.primitive.ReadRegisterGPNode;
import zhixing.jss.cpxInd.individual.primitive.WriteRegisterGPNode;

public class LGPIndividual extends CpxGPIndividual {

	private static final String P_NUMREGISTERS = "numregisters";
	private static final String P_MAXNUMTREES = "maxnumtrees";
	private static final String P_MINNUMTREES = "minnumtrees";
	
	private static final String P_INITMAXNUMTREES = "init_maxnumtrees";
	private static final String P_INITMINNUMTREES = "init_minnumtrees";
	
	private static final String P_RATEFLOWOPERATOR = "rate_flowoperator";
	private static final String P_MAXITERTIMES = "max_itertimes";
	
	private static final String P_NUMOUTPUTREGISTERS = "num-output-register";
	private static final String P_OUTPUTREGISTER = "output-register";
	
	private static final String P_EFFECTIVE_INITIAL = "effective_initial";
	
	private Parameter privateParameter;
	
	protected int MaxNumTrees;
	protected int MinNumTrees;
	
	protected int initMaxNumTrees;
	protected int initMinNumTrees;
	
	protected int numRegs;
	protected int numOutputRegs;
	
	protected double rateFlowOperator;
	protected int maxIterTimes;
	
	protected boolean eff_initialize;
	
	protected double registers [] = null;
	//protected double constant_registers[] = null;
	
	protected ArrayList<GPTreeStruct> trees;
	
	protected LGPFlowController flowctrl;
	
	protected int [] outputRegister;
	
	protected int fastFlag;
	private ArrayList<GPTreeStruct> exec_trees;
	protected int initReg[] = null;
	protected int init_ConReg[] = null;
	protected AttributeGPNode initReg_values[] = null;
	
	protected byte constraintsNum = 0;
	
	@Override
	public void setup(final EvolutionState state, final Parameter base) {
		//set up the individual prototype 
		 super.setup(state,base); 
		 Parameter def = defaultBase();
	     
		 // set my evaluation to false
		 evaluated = false;
	    
		 // the maximum/minimum number of trees
	     MaxNumTrees = state.parameters.getInt(base.push(P_MAXNUMTREES),def.push(P_MAXNUMTREES),1);  // at least 1 tree for GP!
	     if (MaxNumTrees <= 0) 
	         state.output.fatal("An LGPIndividual must have at least one tree.",
	             base.push(P_MAXNUMTREES),def.push(P_MAXNUMTREES));
	     
	     MinNumTrees = state.parameters.getInt(base.push(P_MINNUMTREES),def.push(P_MINNUMTREES),1);  // at least 1 tree for GP!
	     if (MinNumTrees <= 0) 
	         state.output.fatal("An LGPIndividual must have at least one tree.",
	             base.push(P_MINNUMTREES),def.push(P_MINNUMTREES));
	     
	     initMaxNumTrees = state.parameters.getInt(base.push(P_INITMAXNUMTREES),def.push(P_INITMAXNUMTREES),1);  // at least 1 tree for GP!
	     if (MaxNumTrees <= 0) 
	         state.output.fatal("An LGPIndividual must have at least one tree.",
	             base.push(P_INITMAXNUMTREES),def.push(P_INITMAXNUMTREES));
	     
	     initMinNumTrees = state.parameters.getInt(base.push(P_INITMINNUMTREES),def.push(P_INITMINNUMTREES),MinNumTrees);  // at least 1 tree for GP!
	     if (MinNumTrees <= 0) 
	         state.output.fatal("An LGPIndividual must have at least one tree.",
	             base.push(P_INITMINNUMTREES),def.push(P_INITMINNUMTREES));
	     
	     numRegs = state.parameters.getInt(base.push(P_NUMREGISTERS),def.push(P_NUMREGISTERS),1);  // at least 1 register for GP!
	     //numRegs = ((GPInitializer)state.initializer).treeConstraints[0].functionset.registers[0].length;  
	     		//first 0 index: index of tree constraints, second 0 index: index of register design
	     if (numRegs <= 0) 
	         state.output.fatal("An LGPIndividual must have at least one register.",
	             base.push(P_NUMREGISTERS),def.push(P_NUMREGISTERS));
	     
	     rateFlowOperator = state.parameters.getDoubleWithDefault(base.push(P_RATEFLOWOPERATOR), def.push(P_RATEFLOWOPERATOR), 0.);
	     if(rateFlowOperator < 0 || rateFlowOperator > 1){
	    	 state.output.fatal("the rate of flow operator must be >=0 and <=1.",
	    			 base.push(P_RATEFLOWOPERATOR), def.push(P_RATEFLOWOPERATOR));
	     }
	     
	     maxIterTimes = state.parameters.getIntWithDefault(base.push(P_MAXITERTIMES), def.push(P_MAXITERTIMES), 100);
	     if(maxIterTimes <=0){
	    	 state.output.fatal("max iteration times must be >=1", base.push(P_MAXITERTIMES), def.push(P_MAXITERTIMES));
	     }
	     
	     eff_initialize = state.parameters.getBoolean(base.push(P_EFFECTIVE_INITIAL), def.push(P_EFFECTIVE_INITIAL), false);
	     
	     numOutputRegs = state.parameters.getIntWithDefault(base.push(P_NUMOUTPUTREGISTERS),def.push(P_NUMOUTPUTREGISTERS),1);
	     if (numOutputRegs <= 0) 
	         state.output.fatal("An LGPIndividual must have at least one output register.",
	             base.push(P_NUMOUTPUTREGISTERS),def.push(P_NUMOUTPUTREGISTERS));
	     outputRegister = new int[numOutputRegs];
	     for(int r = 0; r<numOutputRegs; r++){
	    	 Parameter b = base.push(P_OUTPUTREGISTER).push("" + r);
	            
            int reg = state.parameters.getIntWithDefault(b, null, 0);
            if(reg < 0 ){
            	System.err.println("ERROR:");
                System.err.println("output register must be >= 0.");
                System.exit(1);
            }
            outputRegister[r] = reg;
	     }
	     
	     // load the trees
	     trees = new ArrayList<>();
	     exec_trees = new ArrayList<>();

	     for (int x=0;x<MaxNumTrees;x++)
         {
            Parameter p = base.push(P_TREE).push(""+0);
            privateParameter = p;
            GPTreeStruct t = (GPTreeStruct)(state.parameters.getInstanceForParameterEq(
                    p,def.push(P_TREE).push(""+0),GPTreeStruct.class));
            t.owner = this; 
            t.status = false;
            t.effRegisters = new HashSet<Integer>(0);
            t.setup(state,p);
            trees.add(t);
            
            constraintsNum = t.constraints;
         }
	     
	     //initialize registers
	     registers = new double [numRegs];
	     //resetRegisters();
	     //constant_registers = new double [JobShopAttribute.values().length];
	     
	     //flow controller
	     flowctrl = new LGPFlowController();
	     flowctrl.maxIterTimes = maxIterTimes;
	     
	     // now that our function sets are all associated with trees,
        // give the nodes a chance to determine whether or not this is
        // going to work for them (especially the ADFs).
        GPInitializer initializer = ((GPInitializer)state.initializer);
        int x = 0;
        for (GPTreeStruct tree: trees)
            {
            for(int w = 0;w < tree.constraints(initializer).functionset.nodes.length;w++)
                {
                GPNode[] gpfi = tree.constraints(initializer).functionset.nodes[w];
                for (int y = 0;y<gpfi.length;y++)
                    gpfi[y].checkConstraints(state,x++,this,base);
                }
            }
        // because I promised with checkConstraints(...)
        state.output.exitIfErrors();
	}
	
	public void adjustTreesLength(EvolutionState state, int thread, int numtrees) {
		if(numtrees < trees.size()) {
			int cnt = trees.size() - numtrees;
			for(int i = 0;i<cnt;i++) {
				int index = state.random[thread].nextInt(trees.size());
				trees.remove(index);
				
			}
		}
		else if(numtrees > trees.size()) {
			int cnt = numtrees - trees.size();
			for(int i = 0;i<cnt;i++) {
				int index = state.random[thread].nextInt(trees.size());
				GPTreeStruct newtGpTree = (GPTreeStruct)(state.parameters.getInstanceForParameterEq(
	                    privateParameter,defaultBase().push(P_TREE).push(""+0),GPTreeStruct.class));
				newtGpTree.owner = this;
				newtGpTree.status = false;
				newtGpTree.effRegisters = new HashSet<>(0);
				newtGpTree.setup(state, privateParameter);
				trees.add(index, newtGpTree);
			}
		}
		
		updateStatus();
	}
	
	public void rebuildIndividual(EvolutionState state, int thread) {
		int numtrees = state.random[thread].nextInt(initMaxNumTrees - initMinNumTrees + 1) + initMinNumTrees;
		
		trees.clear();
		
		for(int i =0;i<numtrees;i++){
			//GPTreeStruct tree = new GPTreeStruct();
			GPTreeStruct tree = (GPTreeStruct)(state.parameters.getInstanceForParameterEq(
                    privateParameter,defaultBase().push(P_TREE).push(""+0),GPTreeStruct.class));
			tree.constraints = this.constraintsNum;
			tree.buildTree(state, thread);
			trees.add(tree);
		}
		
		
		updateStatus();
		
		if(eff_initialize){//if we have to ensure all the instructions are effective in the initialization
			this.removeIneffectiveInstr();
			int trial = 100*this.initMaxNumTrees;
			while(countStatus()<numtrees && trial>0){
				GPTreeStruct tree = (GPTreeStruct)(state.parameters.getInstanceForParameterEq(
	                    privateParameter,defaultBase().push(P_TREE).push(""+0),GPTreeStruct.class));
				tree.constraints = this.constraintsNum;
				tree.buildTree(state, thread);
				trees.add(0,tree);
				updateStatus();
				this.removeIneffectiveInstr();
				trial --;
			}
		}

	}
	
	public double[] getRegisters(){
		return registers;
	}
	
	public double getRegisters(int i){
		return registers[i];
	}
	
//	public double[] getConstantRegisters(){
//		return constant_registers;
//	}
	
	public int[] getOutputRegisters(){
		return outputRegister;
	}
	
	public void setOutputRegisters(int[] tar){
		outputRegister = new int [tar.length];
		int i = 0;
		for(int t : tar){
			outputRegister[i] = t;
			i++;
		}
	}
	
	public void resetIndividual(int numReg, int maxIterTime) {
		List<Integer> tmp = new ArrayList<>();
	     tmp.add(0);
	     resetIndividual(numReg, maxIterTime, tmp);
	}
	
	public void resetIndividual(int numReg, int maxIterTime, List<Integer> outReg){
		//numReg: the maximum number of registers,  maxIterTime: the maximum iteration time of loop structures,  outReg: output register
		numRegs = numReg;
		 maxIterTimes = maxIterTime;
		 // set my evaluation to false
		 evaluated = false;
		
		 //initialize registers
	     registers = new double [numRegs];
	     //resetRegisters();
	     
	     //flow controller
	     flowctrl = new LGPFlowController();
	     flowctrl.maxIterTimes = maxIterTimes;
	     
	     // load the trees
	     trees = new ArrayList<>();
	     
	     outputRegister = new int[outReg.size()]; //by default, only one output register and the first register is the output
	     for(int i=0;i<outReg.size();i++){
	    	 outputRegister[i] = outReg.get(i);
	     }
	     
	}
	
	public void setRegister(int ind, double value){
		registers[ind] = value;
	}
	
	public void resetRegisters(final Problem problem){
		resetRegisters(problem, 1);
	}
	
	public void resetRegisters(final Problem problem, double val){

		DoubleData tmp = new DoubleData();
		
		for(int i = 0;i<numRegs;i++){
			if(initReg[i] == 1){
//				JobShopAttribute a = list[i];
//				(new AttributeGPNode(a)).eval(null, 0, tmp, null, this, problem);
				initReg_values[i].eval(null, 0, tmp, null, this, problem);
				registers[i] = tmp.value;
			}
			else{
				registers[i] = val;
			}
		}
	}
	
	public int getMaxNumTrees(){
		return MaxNumTrees;
	}
	
	public int getMinNumTrees(){
		return MinNumTrees;
	}
	
	public FlowController getFlowController() {
		return flowctrl;
	}
	
	public double getrateFlowOperator() {
		return rateFlowOperator;
	}
	
	public  boolean equals(Object ind){
		if (ind == null) return false;
        if (!(this.getClass().equals(ind.getClass()))) return false;  // LGPIndividuals are special.
        LGPIndividual i = (LGPIndividual)ind;
        if (trees.size() != i.trees.size()) return false;
        // this default version works fine for most GPIndividuals.
        for(int x=0;x<trees.size();x++)
            if (!(trees.get(x).treeEquals(i.trees.get(x)))) return false;

        return true;
	}
	
	
	public  int hashCode(){
		// stolen from GPNode.  It's a decent algorithm.
        int hash = this.getClass().hashCode();
        
        for(int x=0;x<trees.size();x++)
            hash =
                // Rotate hash and XOR
                (hash << 1 | hash >>> 31 ) ^
                trees.get(x).treeHashCode();
        return hash;
	}
	
	
	public  void verify(EvolutionState state){
		if (!(state.initializer instanceof GPInitializer))
        { state.output.error("Initializer is not a CpxGPInitializer"); return; }
        
	    // GPInitializer initializer = (GPInitializer)(state.initializer);
	
	    if (trees==null) 
	        { state.output.error("Null trees in CpxGPIndividual."); return; }
	    int x = 0;
	    for(GPTreeStruct tree: trees) {
	    	if (tree==null) 
	        { state.output.error("Null tree (#"+x+") in CpxGPIndividual."); return; }
	    	x++;
	    }
	    for(GPTreeStruct tree: trees)
	        tree.verify(state);
	    state.output.exitIfErrors();
	}
	
	
	public  void printTrees(final EvolutionState state, final int log){
		int x = 0;
		for(GPTreeStruct tree: trees)
        {
			if(!tree.status) {
				state.output.print("//", log);
			}
	        state.output.print("Ins " + x + ":\t",log);
	        if(tree.type == 0) {
	        	 tree.printTreeForHumans(state,log);
	        }
	        else {
	        	//it is flow control instruction
	        	tree.child.children[0].printRootedTree(state, log, Output.V_VERBOSE);
	        	state.output.println("",log);
	        }
	        x++;
        }
	}
	
	
	public  void printIndividualForHumans(final EvolutionState state, final int log){
		state.output.println(EVALUATED_PREAMBLE + (evaluated ? "true" : "false"), log);
        fitness.printFitnessForHumans(state,log);
        printTrees(state,log);
        int cnteff = countStatus();
        state.output.println("# Effective instructions:\t"+cnteff+"\teffective %:\t"+((double)cnteff)/trees.size()*100, log);
	}
	
	
	public  void printIndividual(final EvolutionState state, final int log){
		state.output.println(EVALUATED_PREAMBLE + Code.encode(evaluated), log);
        fitness.printFitness(state,log);
        int x = 0;
        for(GPTreeStruct tree : trees)
            {
            state.output.println("Ins " + x + ":",log);
            tree.printTree(state,log);
            x++;
            }   
	}
	
	
	public  void printIndividual(final EvolutionState state, final PrintWriter writer){
		writer.println(EVALUATED_PREAMBLE + Code.encode(evaluated));
        fitness.printFitness(state,writer);
        int x = 0;
        for(GPTreeStruct tree:trees)
            {
            writer.println("Ins " + x + ":");
            tree.printTree(state,writer);
            x++;
            }   
	}
	
	/** Overridden for the GPIndividual genotype. */
	
    public  void writeGenotype(final EvolutionState state,
        final DataOutput dataOutput) throws IOException
    {
	    dataOutput.writeInt(trees.size());
	    for(GPTreeStruct tree : trees)
	        tree.writeTree(state,dataOutput);
    }

    /** Overridden for the GPIndividual genotype. */
	
    public  void readGenotype(final EvolutionState state,
        final DataInput dataInput)throws IOException
    {
	    int treelength = dataInput.readInt();
	    if(treelength > MaxNumTrees || treelength < MinNumTrees) {
	    	state.output.fatal("Number of trees is inconsistent with the given Max / Min NumTrees.");
	    }
	    if (trees == null)
	        state.output.fatal("null trees collections!");
	    
	    adjustTreesLength(state, 0, treelength);
	    
	    for(int x=0;x<trees.size();x++)
	        trees.get(x).readTree(state,dataInput);
    }

	
    public  void parseGenotype(final EvolutionState state,
        final LineNumberReader reader)throws IOException
    {
    	//suppose the tree in readLine has a same number of trees with the individual 
	    // Read my trees
	    for(int x=0;x<trees.size();x++)
	        {
	        reader.readLine();  // throw it away -- it's the tree indicator
	        trees.get(x).readTree(state,reader);
	        }
    }
	
	
	public  Object clone(){
		// a deep clone
		LGPIndividual myobj = (LGPIndividual)(super.clone());
		// copy the trees
        myobj.trees = new ArrayList<GPTreeStruct>();
        for(GPTreeStruct tree : trees)
            {
        	GPTreeStruct t = (GPTreeStruct)(tree.clone());
        	t.owner = myobj;
            myobj.trees.add(t);  // note light-cloned!
            //myobj.trees[x].owner = myobj;  // reset owner away from me
            }
        myobj.copyLGPproperties(this);
        return myobj;
	}
	
	protected void copyLGPproperties(LGPIndividual obj){
		this.numRegs = obj.getRegisters().length;
		this.MaxNumTrees = obj.getMaxNumTrees();
		this.MinNumTrees = obj.getMinNumTrees();
		this.registers = new double[numRegs];
		for(int i = 0; i<numRegs; i++){
			this.registers[i] = obj.getRegisters()[i];
		}
		
		this.flowctrl = new LGPFlowController();
		this.maxIterTimes = this.flowctrl.maxIterTimes = obj.getFlowController().maxIterTimes;
		
		this.rateFlowOperator = obj.getrateFlowOperator();
	}

	/** Like clone(), but doesn't force the GPTrees to deep-clone themselves. */
	
	public  LGPIndividual lightClone(){
		// a deep clone
		LGPIndividual myobj = (LGPIndividual)(super.clone());
		// copy the trees
        myobj.trees = new ArrayList<GPTreeStruct>();
        for(GPTreeStruct tree : trees)
            {
        	GPTreeStruct t = (GPTreeStruct)(tree.lightClone());
        	t.owner = myobj;
            myobj.trees.add(t);  // note light-cloned!
            //myobj.trees[x].owner = myobj;  // reset owner away from me
            }
        myobj.copyLGPproperties(this);
        return myobj;
	}
	
	/** Returns the "size" of the individual, namely, the number of nodes
	    in all of its subtrees.  */
	
	public  long size(){
		long size = 0;
        for(GPTreeStruct tree : trees)
            size += tree.child.numNodes(GPNode.NODESEARCH_ALL);
        return size;
	}
	
	public double priority(Operation op, WorkCenter workCenter,
            SystemState systemState) {
		//it is used in Job Shop Scheduling, to prioritize the operations in a machine queue
		CalcPriorityProblem calcPrioProb =
		 new CalcPriorityProblem(op, workCenter, systemState);
		
		DoubleData tmp = new DoubleData();
		
		return execute(null, 0, tmp, null, this, calcPrioProb);
	}
	
	public double execute(EvolutionState state, int thread, GPData input, ADFStack stack, GPIndividual individual, Problem problem) {
		//execute the whole individual. the fastFlag means that we will not consider the flow of instructions, just execute instructions one-by-one
		//the fastFlag should be set true only when there is not flow control operators in the primitive set.
		
		resetRegisters(problem, 1); //initialize registers by input features (or the default value)
		
		if(fastFlag == 1){
			DoubleData rd = ((DoubleData)(input));
			for(GPTreeStruct tree : exec_trees) {
				tree.child.eval(state, thread, input, stack, individual, problem);
		}
		}
		else{
			flowctrl.execute(state, thread, input, stack, (CpxGPIndividual)individual, problem);
		}
			
		
		return registers[outputRegister[0]]; //only the first output register will be used in basic LGP
	}
	
	@Override
	public GPTree getTree(int index){
		if(index >= trees.size()){
    		System.out.println("The tree index " + index + " is out of range");
    		System.exit(1);
    	}
    	return trees.get(index);
	}
	
	public GPTreeStruct getTreeStruct(int index) {
		if(index >= trees.size()){
    		System.out.println("The tree index " + index + " is out of range");
    		System.exit(1);
    	}
    	return trees.get(index);
	}
	
	public List<GPTreeStruct> getTreeStructs(){
		return trees;
	}
	
	@Override
	public boolean setTree(int index, GPTree tree){
		if(index < trees.size()){
			trees.remove(index);
			GPTreeStruct treeStr = (GPTreeStruct)tree;
			if(!(tree instanceof GPTreeStruct)) {
				treeStr.status = false;
				treeStr.effRegisters = new HashSet<>(0);
			}
			treeStr.type = 0;
			if(treeStr.child.children[0] instanceof FlowOperator){
				if(treeStr.child.children[0] instanceof Branching){
					treeStr.type = 1;
				}
				else{
					treeStr.type = 2;
				}
			}
			trees.add(index, treeStr);
			
			updateStatus();
			
			return true;
		}
		System.out.println("setTree index: " + index + " is out of range " + trees.size());
		return false;
	}
	
	public void addTree(int index, GPTree tree){
		//add "tree" to the index slot
		GPTreeStruct treeStr;
		if(tree instanceof GPTreeStruct){
			treeStr = (GPTreeStruct) tree;
		}
		else{
			treeStr = new GPTreeStruct();
			treeStr.assignfrom(tree);
			treeStr.status = false;
			treeStr.effRegisters = new HashSet<>(0);
		}
		
		treeStr.type = 0;
		if(treeStr.child.children[0] instanceof FlowOperator){
			if(treeStr.child.children[0] instanceof Branching){
				treeStr.type = 1;
			}
			else{
				treeStr.type = 2;
			}
		}
		if(index < 0) index = 0;
		if(index < trees.size()) {
			trees.add(index, treeStr);
		}
		else {
			trees.add(treeStr);
		}
		
		updateStatus();
	}
	
	public boolean removeTree(int index) {
		if(index < trees.size()){
			trees.remove(index);
			
			updateStatus();
			
			return true;
		}
		System.out.println("removeTree index: " + index + " is out of range " + trees.size());
		return false;
	}
	
	public boolean removeIneffectiveInstr(){
		for(int ii = 0;ii<this.getTreesLength();ii++) {
			if(!this.getTreeStruct(ii).status && this.getTreesLength()>this.getMinNumTrees()) {
				this.removeTree(ii);
				ii--; //ii remain no change, so that it can point to the next tree
			}
		}
		return true;
	}
	
	@Override
	public int getTreesLength(){
		return trees.size();
	}
	
	public int getEffTreesLength(){
		updateStatus();
		int res = 0;
		for(GPTreeStruct tree : trees){
			if (tree.status){
				res ++;
			}
		}
		
		return res;
	}
	
	public void updateStatus(int n, int []tar) {
		//identify which instructions are extrons and vise versa
		//start to update the status from position n
		//tar: the output register
		
		boolean statusArray [] = new boolean [trees.size()];
		boolean sourceArray [][] = new boolean [trees.size()][numRegs];
		boolean destinationArray [][] = new boolean [trees.size()][numRegs];
		
		Set<Integer> source, destination;
		
		if(n > trees.size()) {
			System.out.println("The n in updateStatus is larger than existing tree list");
			System.exit(1);
		}
		
		ListIterator<GPTreeStruct> it = trees.listIterator();
		int cn = n;
		while(it.hasNext() && cn > 0) {
			it.next();
			cn --;
		}
		
		//initialize target effective registers
		Set<Integer> targetRegister = new HashSet<>(0);
		for(int i = 0; i<tar.length; i++)
			targetRegister.add(tar[i]);
		
		//backward loop
		cn = n - 1;  //serve as index
		while(it.hasPrevious()) {
			GPTreeStruct tree = (GPTreeStruct) it.previous();
			
			tree.effRegisters = new HashSet<>(0);
			Iterator<Integer> itt = targetRegister.iterator();
        	while(itt.hasNext()) {
        		int v = itt.next();
        		tree.effRegisters.add(v);
        	}
			
			//check it is effective
			if(tree.child.children[0] instanceof Branching){
				//branching. if its body contain effective instruction, it is effective too.
				tree.type = 1;
				tree.status = statusArray[cn] = false;
				for(int i = cn + 1; i < trees.size() && i <= ((FlowOperator)tree.child.children[0]).getBodyLength()+cn;i++){
					if(statusArray[i]){
						tree.status = statusArray[cn] = true;
						break;
					}
				}
				collectReadRegister(tree.child, sourceArray[cn]);
			}
			else if (tree.child.children[0] instanceof Iteration){
				//iteration. if the effective instructions in its body have a nonempty intersection set
				//of source and destination register
				tree.type = 2;
				source = new HashSet<>();
				destination = new HashSet<>();
				
				//collect arithmetic instruction and their source and destination register
				int j = 0;
				for(int i = cn + 1; i < trees.size(); i++){
					if(statusArray[i] && ! (tree.child.children[0] instanceof FlowOperator) 
							&& j < ((FlowOperator)tree.child.children[0]).getBodyLength()){

						for(int r = 0; r<numRegs; r++){
							if(sourceArray[i][r]) source.add(r);
							if(destinationArray[i][r]) destination.add(r);
						}
						j++;
						if(j>=((FlowOperator)tree.child.children[0]).getBodyLength()) break;
					}
				}
				source.retainAll(destination);
				if(!source.isEmpty()){
				    tree.status = statusArray[cn]=true;
				}
				else {
					tree.status = statusArray[cn]=false;
				}
				collectReadRegister(tree.child, sourceArray[cn]);
			}
			else {
				tree.type = 0;
				
				
				if(targetRegister.contains(((WriteRegisterGPNode) tree.child).getIndex())){
					tree.status = true;
					targetRegister.remove(((WriteRegisterGPNode) tree.child).getIndex());
					tree.updateEffRegister(targetRegister);
					
					statusArray[cn] = true;
				}
				else {
					tree.status = false;
					
					statusArray[cn] = false;
				}
				
				destinationArray[cn][((WriteRegisterGPNode) tree.child).getIndex()] = true;
				collectReadRegister(tree.child, sourceArray[cn]);
			} 
			cn--;
		}
		
	}
	
	public void updateStatus() {
		updateStatus(trees.size(),outputRegister);
	}
	
	public ArrayList<Integer> getSubGraph(int n, Integer [] tar){
		//return the indices of the instructions which form the sub-graph.
		//the sub-graph is searched from index "n", with the target output indicated by "tar".
		
		updateStatus();
		
		if(n > trees.size()) {
			System.out.println("The n in updateStatus is larger than existing tree list");
			System.exit(1);
		}
		
		ArrayList<Integer> graph = new ArrayList<>();
		
		//initialize target effective registers
		Set<Integer> targetRegister = new HashSet<>(0);
		for(int i = 0; i<tar.length; i++)
			targetRegister.add(tar[i]);
		
		//backward loop
		int cn = n;
		do{
			GPTreeStruct tree = getTreeStruct(cn);
			if( tree.status && targetRegister.contains(((WriteRegisterGPNode) tree.child).getIndex())){
				graph.add(cn);
				
				//update targetRegister
				targetRegister.remove(((WriteRegisterGPNode) tree.child).getIndex());
				tree.updateEffRegister(targetRegister);
			}
			cn --;
		}while(cn >= 0);
		
		return graph;
		
	}
	
	public ArrayList<Integer> getPartialSubGraph(int n, int [] tar, double rate, final EvolutionState state, final int thread){
		//return the indices of the instructions which form the sub-graph based on a probability "rate". this function might randomly abandon some graph nodes
		//the sub-graph is searched from index "n", with the target output indicated by "tar".
		
		updateStatus();
		
		if(n > trees.size()) {
			System.out.println("The n in updateStatus is larger than existing tree list");
			System.exit(1);
		}
		
		ArrayList<Integer> graph = new ArrayList<>();
		
		//initialize target effective registers
		Set<Integer> targetRegister = new HashSet<>(0);
		for(int i = 0; i<tar.length; i++)
			targetRegister.add(tar[i]);
		
		//backward loop
		int cn = n;
		do{
			GPTreeStruct tree = getTreeStruct(cn);
			if( tree.status && targetRegister.contains(((WriteRegisterGPNode) tree.child).getIndex())){
				graph.add(cn);
				
				//update targetRegister
				targetRegister.remove(((WriteRegisterGPNode) tree.child).getIndex());
				
				Set<Integer> s = tree.collectReadRegister();
				Iterator<Integer> it = s.iterator();
				while(it.hasNext()){
					it.next();
					if(s.size()>1 && state.random[thread].nextDouble()>rate){
						it.remove();
					}
				}
				
				for(Integer r : s){
					targetRegister.add(r);
				}
			}
			cn --;
		}while(cn >= 0);
		
		return graph;
		
	}
	
	public ArrayList<Integer> getIntersecSubGraph(Set<Integer> tar, int maintarget){
		ArrayList<Integer> res=null, res_tmp=null, tmp=null;
		
		res = this.getSubGraph(this.getTreesLength()-1, new Integer[]{maintarget});
		
		for(int output : tar){
			tmp = this.getSubGraph(this.getTreesLength()-1, new Integer[]{output});
			if(res_tmp == null){
				res_tmp = (ArrayList<Integer>) tmp.clone();
			}
			else{
				res_tmp.retainAll(tmp);
			}
		}
		
		if(res_tmp.size() == 0)
			return res;
		else
			return res_tmp;
	}
	
	public int countStatus(){
		int cnt = 0;
		for(GPTreeStruct tree:trees){
			if(tree.status){
				cnt++;
			}
		}
		return cnt;
	}
	
	public int countStatus(int start, int end){
		//return the number of effective instructions in [start, end)
		int cnt = 0;
		for(int i = start; i<end; i++){
			GPTreeStruct tree = trees.get(i);
			if(tree.status){
				cnt++;
			}
		}
		return cnt;
	}
	
	public boolean canAddFlowOperator(){
		boolean res = true;
		int cnt = 0;
		for(GPTreeStruct tree:trees){
			if(tree.child.children[0] instanceof FlowOperator){
				cnt++;
			}
		}
		if(((double)cnt)/trees.size() > rateFlowOperator) res = false;
		return res;
	}
	
	protected void collectReadRegister(GPNode node, boolean[] collect){
		//collect the source registers for an instruction
		//node: an instruction primitive
		if(node instanceof ReadRegisterGPNode){
			collect[((ReadRegisterGPNode) node).getIndex()] = true;
		}
		else{
			if(node.children.length > 0){
				for(int n =0;n<node.children.length;n++){
					collectReadRegister(node.children[n], collect);
				}
			}
		}
		return;
	}
	
	public String makeGraphvizRule(List<Integer> outputRegs){
		//this function is not support the instructions whose depth is larger than 3, also not support "IF" operation since
		//DAG cannot tell the loop body. If there are more than one operation in one instruction, subgraph of Graphviz should be used
		
		//collect terminal names
		String usedTerminals[] = new String[numRegs];
		for(int j = 0; j<numRegs;) {
			for (JobShopAttribute a : JobShopAttribute.relativeAttributes()) {
				
				usedTerminals[j++] = a.getName();

				if(j>=numRegs) break;
			}
		}
		
		Set<String> JSSAttributes = new HashSet<>();
		
		//check all instructions and specify all effective operations, effective constants 
		String nodeSpec ="";
		for(int i = 0;i<trees.size();i++){
			GPTreeStruct tree = trees.get(i);
			
			if(!tree.status) continue;
			
			nodeSpec += "" + i + "[label=\"" + tree.child.children[0].toString() + "\"];\n";
			for(int c = 0;c<tree.child.children[0].children.length; c++){
				GPNode node = tree.child.children[0].children[c];
				if(node instanceof AttributeGPNode){
					//check whether it has been here
					if(!JSSAttributes.contains(node.toString())){
						JSSAttributes.add(node.toString());
						nodeSpec += node.toString()+"[shape=box];\n";
					}
					nodeSpec += "" + i + "->"+node.toString()+"[label=\"" + c +"\"];\n";
				}
			}
		}
		
		//backward visit all effective instructions, connect the instructions
		String connection = "";
		Set<Integer> notUsed = new HashSet<>(outputRegs);
		for(int i=trees.size()-1;i>=0;i--){
			GPTreeStruct tree = trees.get(i);
			
			if(!tree.status) continue;
			
			if(notUsed.contains((((WriteRegisterGPNode) tree.child)).getIndex())){
				connection += "R" +  (((WriteRegisterGPNode) tree.child)).getIndex() +"[shape=box];\n";
				connection += "R" +  (((WriteRegisterGPNode) tree.child)).getIndex() + "->" + i + ";\n";
				notUsed.remove((Integer)(((WriteRegisterGPNode) tree.child)).getIndex());
			}
			
			//find the instructions whose destination register is the same with the source registers for this instruction
			List<Integer> source = new ArrayList<>();
			for(int c = 0;c<tree.child.children[0].children.length; c++){
				GPNode node = tree.child.children[0].children[c];
				if(node instanceof ReadRegisterGPNode){
					source.add(((ReadRegisterGPNode)node).getIndex());
					
					for(int j = i-1;j>=0;j--){
						
						GPTreeStruct visit = trees.get(j);
						
						if(!visit.status) continue;
						
						while(source.contains((((WriteRegisterGPNode) visit.child)).getIndex())){
							connection += "" + i + "->" + j + "[label=\"" + c +"\"];\n";
							source.remove(source.indexOf((((WriteRegisterGPNode) visit.child)).getIndex()));
						}
						
						if(source.size()==0) break;
					}
					//if there is still source registers, connect the instruction with JSS attributes
					for(Integer j : source){
						connection += usedTerminals[j]+"[shape=box];\n";   // use job shop attributes to initialize registers
						connection += "" + i + "->" + usedTerminals[j] + "[label=\"" + c +"\"];\n";
//						connection += "1[shape=box];\n";  // use "1" to initialize registers
//						connection += "" + i + "->" + "1[label=\"" + c +"\"];\n";
					}
					source.clear();
				}
				
				
			}
			
		}
		
		String result = "digraph g {\n" 
		+"nodesep=0.2;\n"
		+"ranksep=0;\n"
		+ "node[fixedsize=true,width=1.3,height=0.6,fontsize=\"30\",fontname=\"times-bold\",style=filled, fillcolor=lightgrey];\n"
		+"edge[fontsize=\"25.0\",fontname=\"times-bold\"];\n"
		+ nodeSpec
		+ connection
		+ "}\n";
		
		return result;
	}
	
	
	public String makeGraphvizRule(){
		return makeGraphvizRule(null);
	}
	
	public double getMeanEffDepenDistance(){
		ArrayList<Integer> DependenceDis = new ArrayList<>();
		
		
		
		for(int i = 0;i<getTreesLength();i++){
			GPTreeStruct tree = getTreeStruct(i);
			
			if(!tree.status || tree.type !=0) continue;  //ineffective or not arithmetic instruction
			
			int j = 1;
			for(;i+j<getTreesLength();j++){
				if(!getTreeStruct(i+j).status || getTreeStruct(i+j).type !=0) continue;  //ineffective or not arithmetic instruction
				
				if(getTreeStruct(i+j).collectReadRegister().contains(((WriteRegisterGPNode)tree.child).getIndex())){
					DependenceDis.add(j);
					break;
				}
			}
		}
		
		if(DependenceDis.size()>0){
			double res = 0;
			for(Integer a : DependenceDis){
				res += a;
			}
			return res / DependenceDis.size();
		}
		else
			return 0;
	}
	
	public ArrayList<Double> getEffDegree(){
		ArrayList<Double> degreeList = new ArrayList<>();
		
		//arithmetic instructions
		for(int i = 0;i<getTreesLength();i++){
			degreeList.add(0.0);
			GPTreeStruct tree = getTreeStruct(i);
			
			if(!tree.status || tree.type !=0) continue;  //ineffective or not arithmetic instruction
			
			
			for(int j = 1;i+j<getTreesLength();j++){
				if(!getTreeStruct(i+j).status || getTreeStruct(i+j).type !=0) continue;  //ineffective or not arithmetic instruction
				
				List<Integer> tmp = getTreeStruct(i+j).collectReadRegister_list();
				
				if(tmp.contains(((WriteRegisterGPNode)tree.child).getIndex())){
					for(Integer t : tmp){
						if(t == ((WriteRegisterGPNode)tree.child).getIndex()){
							degreeList.set(i, degreeList.get(i)+1);
						}
					}
				}
				
				//check the destination register, if the WriteRegister of instruction i is written, break;
				if(((WriteRegisterGPNode)getTreeStruct(i+j).child).getIndex() == ((WriteRegisterGPNode)tree.child).getIndex()){
					break;
				}
			}
		}
		
		//branching and iteration instruction, take the average dependence degree in the branched instructions as their dependence degree
		for(int i = 0;i<getTreesLength();i++){
			GPTreeStruct tree = getTreeStruct(i);
			
			if(!tree.status || tree.type ==0) continue; 
			
			int bodyLength = ((FlowOperator)tree.child.children[0]).getBodyLength();
			
			double degree = 0;
			double cnt = 0;
			
			for(int j = 1;j<=bodyLength && i+j < getTreesLength();j++){
				if(!getTreeStruct(i+j).status) continue;
				
				//extend the body length if there is a nested one
				if(getTreeStruct(i+j).type != 0) 
					bodyLength += ((FlowOperator)getTreeStruct(i+j).child.children[0]).getBodyLength();
				else {
					degree += degreeList.get(i+j);
					cnt ++;
				}
			}
			
			degreeList.set(i, degree / cnt);
			
		}
		return degreeList;
	}
	
	public ArrayList<Integer> getNumEffRegister(){
		ArrayList<Integer> EffRegList = new ArrayList<>();
		
		updateStatus();
		
		for(GPTreeStruct tree : trees){
			EffRegList.add(tree.effRegisters.size());
		}
		
		return EffRegList;
	}
	
	public int getApproximateGraphWidth(int index) {
		ArrayList<Integer> tmp_list = getNumEffRegister();
		if(index >= tmp_list.size()) return tmp_list.get(tmp_list.size()-1);
		if(index < 0) return tmp_list.get(0);
		
		return tmp_list.get(index);
	}
	
	public int getEffectiveIndex(int index) {
		//inputs: instruction index in genome
		//outputs: the index with only effective instructions. if it is an intron, returns the index of the closest effective instruction with smaller structural index;
		//if index exceeds the maximum index, return # effective instructions (i.e., the root), if it is smaller than the minimum index, returns 0;
		if(index < 0) return 0;
		if(index >= getTreesLength()) return getEffTreesLength()-1;
		
		if(!getTreeStruct(index).status) {
			int i;
			for(i = index - 1; i>=0; i--) {
				if(getTreeStruct(i).status) {
					return getEffectiveIndex(i);
				}
			}
			if(i<0) return 0;
		}
		
		int res = -1;
		
		for(int i = 0;i<=index;i++) {
			if(getTreeStruct(i).status) res++;
		}
		
		return res;
	}
	
	@Override
	public void prepareExecution(){
		//check whehter we need flow controller
		//if instruction type of all effective instructions is 0, set fast mode
		fastFlag = 1; // 1: fast mode,  0: slow mode
		for(GPTreeStruct tree : trees) {
			if(tree.status && tree.type != 0) {  //effective and not arithmetic (branching or iteration)
				fastFlag = 0;
				break;
			}
		}
		
		if(exec_trees == null){
			exec_trees = new ArrayList<>();
		}
		else{
			exec_trees.clear();
		}
		
		
		//check which registers are necessarily to be initialized.
		initReg = new int [numRegs];
		//init_ConReg = new int [JobShopAttribute.values().length];
		for(int i = 0;i<numRegs;i++){
			initReg[i] = -1;
		}
		for(GPTreeStruct tree : trees){
			if(!tree.status) continue;
			
			for(int c = 0;c<2;c++){
				GPNode node = tree.child.children[0].children[c];
				if(node instanceof ReadRegisterGPNode){
					int ind = ((ReadRegisterGPNode)node).getIndex();
					if(initReg[ind]==-1){ //have not been written or read
						initReg[ind] = 1;  //it is read before being written, necessary to be initialized. 
					}
				}
				
				//check which constant register is necessarily to be initialized.
//				if(node instanceof ReadConstantRegisterGPNode){
//					init_ConReg[((AttributeGPNode)((TerminalERC)node).getTerminal()).getJobShopAttribute().ordinal()] = 1;
//				}
			}
			int ind = ((WriteRegisterGPNode)tree.child).getIndex();
			if(initReg[ind] == -1){ //have not been written or read
				initReg[ind] = 0; //it is wirtten before being read, unnecessary to be initialized
			}
			
			
			if(fastFlag == 1){
				exec_trees.add(tree);
			}
		}
		
		if(registers == null) {
			registers = new double[numRegs];
		}
//		if(constant_registers == null){
//			constant_registers = new double [JobShopAttribute.values().length];
//		}
		
		//identify the initialization input features for the registers. 
		JobShopAttribute list[] = JobShopAttribute.relativeAttributes();
	     initReg_values = new AttributeGPNode [numRegs];
		 for(int i = 0;i<numRegs;i++){
			 JobShopAttribute a = list[i % list.length];
			 initReg_values[i] = new AttributeGPNode(a);
		 }
	}
	
//	protected void initializeFrequency(EvolutionState state){
//		//get the function set
//		 GPFunctionSet set = this.getTree(0).constraints((GPInitializer)state.initializer).functionset;  //all trees have the same function set	
//		 
//		 //collect the name of all functions and constants
//		 //functions
//		 int i = 0;
//		 dimension = 0;
//		 dimension += set.nonterminals_v.size();
//		 for(;i<set.nonterminals_v.size();i++){
//			 primitives.add(set.nonterminals_v.get(i).toString());
//		 }
//		//registers
//		 dimension += numRegs;
//		 for(i=0;i<numRegs;i++){
//			 primitives.add("R"+i);
//			 primitives.add("R"+i+"=");
//		 }
//		 //constants
//		 dimension += JobShopAttribute.relativeAttributes().length;
//		 for(i=0;i<JobShopAttribute.relativeAttributes().length;i++){
//			 primitives.add(JobShopAttribute.relativeAttributes()[i].getName());
//		 }
//		 
//		 
//	}
	
//	public double[] getFrequency(int start,int end){
//		//return the functions and terminals' frequency between start and end instruction
//		if(dimension <=0){
//			System.err.print("dimension is less than or equal to 0 in LGPIndividual\n");
//			System.exit(1);
//		}
//		else if(start<0||start>trees.size()-1||end<0||end>trees.size()-1){
//			System.err.print("start or end arguments are out of range in LGPIndividual\n");
//			System.exit(1);
//		}
//		else{
//			frequency = new double[dimension];
//			for(int i = start;i<=end;i++){
//				GPTreeStruct tree = trees.get(i);
//				
//				int si;
//				//get the index of its write register
//				String name = tree.child.toString();
//				si = primitives.indexOf(name);
//				frequency[si] ++;
//				
//				//get the index of its function
//				name = tree.child.children[0].toString();
//				si = primitives.indexOf(name);
//				frequency[si] ++;
//				
//				for(int j = 0;j<tree.child.children[0].expectedChildren();j++){
//					if(tree.child.children[0].children[j] instanceof TerminalERC){
//						name = ((TerminalERC)tree.child.children[0].children[j]).getTerminal().toString();
//						si = primitives.indexOf(name);
//						frequency[si] ++;
//					}
//				}
//			}
//		}
//		return frequency;
//	}
}
