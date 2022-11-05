package zhixing.jss.cpxInd.simulation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.random.RandomDataGenerator;

import zhixing.jss.cpxInd.individual.CpxGPIndividual;
import zhixing.jss.cpxInd.simulation.events.AbstractEvent4Ind;
import zhixing.jss.cpxInd.simulation.events.JobArrivalEvent4Ind;
import ec.EvolutionState;
import yimei.jss.jobshop.Job;
import yimei.jss.jobshop.Objective;
import yimei.jss.jobshop.Operation;
import yimei.jss.jobshop.WorkCenter;
import yimei.jss.simulation.DecisionSituation;
import yimei.util.random.AbstractIntegerSampler;
import yimei.util.random.AbstractRealSampler;
import yimei.util.random.ExponentialSampler;
import yimei.util.random.TwoSixTwoSampler;
import yimei.util.random.UniformIntegerSampler;
import yimei.util.random.UniformSampler;

public class DynamicSimulation4Ind extends Simulation4Ind {
	//lots of private attributes in DynamicSimulation do not be contained in this subclass
		//can change them to protected or use some wrapper method to achieve them
		protected int numAllFinishJobs = 0;

		private List<Job> jobsFinished = new ArrayList<>();

	    public final static int SEED_ROTATION = 10000;

	    private long seed;
	    private RandomDataGenerator randomDataGenerator;

	    private final int minNumOps;
	    private final int maxNumOps;
	    private final double utilLevel;
	    private final double dueDateFactor;
	    private final boolean revisit;

	    private AbstractIntegerSampler numOpsSampler;
	    private AbstractRealSampler procTimeSampler;
	    private AbstractRealSampler interArrivalTimeSampler;
	    private AbstractRealSampler jobWeightSampler;

	    private DynamicSimulation4Ind(long seed,
	                              CpxGPIndividual indi,
	                              int numWorkCenters,
	                              int numJobsRecorded,
	                              int warmupJobs,
	                              int minNumOps,
	                              int maxNumOps,
	                              double utilLevel,
	                              double dueDateFactor,
	                              boolean revisit,
	                              AbstractIntegerSampler numOpsSampler,
	                              AbstractRealSampler procTimeSampler,
	                              AbstractRealSampler interArrivalTimeSampler,
	                              AbstractRealSampler jobWeightSampler) {
	        super(indi, numWorkCenters, numJobsRecorded, warmupJobs);

	        this.seed = seed;
	        this.randomDataGenerator = new RandomDataGenerator();
	        this.randomDataGenerator.reSeed(seed);

	        this.minNumOps = minNumOps;
	        this.maxNumOps = maxNumOps;
	        this.utilLevel = utilLevel;
	        this.dueDateFactor = dueDateFactor;
	        this.revisit = revisit;

	        this.numOpsSampler = numOpsSampler;
	        this.procTimeSampler = procTimeSampler;
	        this.interArrivalTimeSampler = interArrivalTimeSampler;
	        this.jobWeightSampler = jobWeightSampler;

	        setInterArrivalTimeSamplerMean();

	        // Create the work centers, with empty queue and ready to go initially.
	        for (int i = 0; i < numWorkCenters; i++) {
	            systemState.addWorkCenter(new WorkCenter(i));
	        }

	        setup();
	    }

	    public DynamicSimulation4Ind(long seed,
	                             CpxGPIndividual indi,
	                             int numWorkCenters,
	                             int numJobsRecorded,
	                             int warmupJobs,
	                             int minNumOps,
	                             int maxNumOps,
	                             double utilLevel,
	                             double dueDateFactor,
	                             boolean revisit) {
	        this(seed, indi, numWorkCenters, numJobsRecorded, warmupJobs,
	                minNumOps, maxNumOps, utilLevel, dueDateFactor, revisit,
	                new UniformIntegerSampler(minNumOps, maxNumOps),
	                new UniformSampler(1, 99),
	                new ExponentialSampler(),
	                new TwoSixTwoSampler());
	    }

	    public int getNumWorkCenters() {
	        return numWorkCenters;
	    }

	    public int getNumJobsRecorded() {
	        return numJobsRecorded;
	    }

	    public int getWarmupJobs() {
	        return warmupJobs;
	    }

	    public int getMinNumOps() {
	        return minNumOps;
	    }

	    public int getMaxNumOps() {
	        return maxNumOps;
	    }

	    public double getUtilLevel() {
	        return utilLevel;
	    }

	    public double getDueDateFactor() {
	        return dueDateFactor;
	    }

	    public boolean isRevisit() {
	        return revisit;
	    }

	    public RandomDataGenerator getRandomDataGenerator() {
	        return randomDataGenerator;
	    }

	    public AbstractIntegerSampler getNumOpsSampler() {
	        return numOpsSampler;
	    }

	    public AbstractRealSampler getProcTimeSampler() {
	        return procTimeSampler;
	    }

	    public AbstractRealSampler getInterArrivalTimeSampler() {
	        return interArrivalTimeSampler;
	    }

	    public AbstractRealSampler getJobWeightSampler() {
	        return jobWeightSampler;
	    }

	    @Override
	    public void setup() {
	        numJobsArrived = 0;
	        throughput = 0;
	        generateJob();
	    }

	    @Override
	    public void resetState() {
	        systemState.reset();
	        eventQueue.clear();

	        setup();
	    }

	    @Override
	    public void reset() {
	        reset(seed);
	    }

	    public void reset(long seed) {
	        reseed(seed);
	        resetState();
	    }

	    public void reseed(long seed) {
	        this.seed = seed;
	        randomDataGenerator.reSeed(seed);
	    }

	    @Override
	    public void rotateSeed() {
	        seed += SEED_ROTATION; 
	        //seed = (seed + SEED_ROTATION) % (51*SEED_ROTATION);
	        reset();
	    }

	    @Override
	    public void generateJob() {
	        double arrivalTime = getClockTime()
	                + interArrivalTimeSampler.next(randomDataGenerator);
	        double weight = jobWeightSampler.next(randomDataGenerator);
	        Job job = new Job(numJobsArrived, new ArrayList<>(),
	                arrivalTime, arrivalTime, 0, weight);
	        int numOps = numOpsSampler.next(randomDataGenerator);

	        int[] route = randomDataGenerator.nextPermutation(numWorkCenters, numOps);

	        double totalProcTime = 0.0;
	        for (int i = 0; i < numOps; i++) {
	            double procTime = procTimeSampler.next(randomDataGenerator);
	            totalProcTime += procTime;

	            Operation o = new Operation(job, i, procTime, systemState.getWorkCenter(route[i]));

	            job.addOperation(o);
	        }

	        job.linkOperations();

	        double dueDate = job.getReleaseTime() + dueDateFactor * totalProcTime;
	        job.setDueDate(dueDate);

	        systemState.addJobToSystem(job);
	        numJobsArrived ++;

	        eventQueue.add(new JobArrivalEvent4Ind(job));
	    }

	    public double interArrivalTimeMean(int numWorkCenters,
	                                             int minNumOps,
	                                             int maxNumOps,
	                                             double utilLevel) {
	        double meanNumOps = 0.5 * (minNumOps + maxNumOps);
	        double meanProcTime = procTimeSampler.getMean();

	        return (meanNumOps * meanProcTime) / (utilLevel * numWorkCenters);
	    }

	    public void setInterArrivalTimeSamplerMean() {
	        double mean = interArrivalTimeMean(numWorkCenters, minNumOps, maxNumOps, utilLevel);
	        interArrivalTimeSampler.setMean(mean);
	    }

	    public List<DecisionSituation> decisionSituations(int minQueueLength) {
	        List<DecisionSituation> decisionSituations = new ArrayList<>();

	        while (!eventQueue.isEmpty() && throughput < numJobsRecorded) {
	            AbstractEvent4Ind nextEvent = eventQueue.poll();

	            systemState.setClockTime(nextEvent.getTime());
	            nextEvent.addDecisionSituation(this, decisionSituations, minQueueLength);
	        }

	        resetState();

	        return decisionSituations;
	    }

	    @Override
	    public Simulation4Ind surrogate(int numWorkCenters, int numJobsRecorded,
	                                       int warmupJobs) {
	        int surrogateMaxNumOps = maxNumOps;
	        AbstractIntegerSampler surrogateNumOpsSampler = numOpsSampler.clone();
	        AbstractRealSampler surrogateInterArrivalTimeSampler = interArrivalTimeSampler.clone();
	        if (surrogateMaxNumOps > numWorkCenters) {
	            surrogateMaxNumOps = numWorkCenters;
	            surrogateNumOpsSampler.setUpper(surrogateMaxNumOps);

	            surrogateInterArrivalTimeSampler.setMean(interArrivalTimeMean(numWorkCenters,
	                    minNumOps, surrogateMaxNumOps, utilLevel));
	        }

	        Simulation4Ind surrogate = new DynamicSimulation4Ind(seed, individual, numWorkCenters,
	                numJobsRecorded, warmupJobs, minNumOps, surrogateMaxNumOps,
	                utilLevel, dueDateFactor, revisit, surrogateNumOpsSampler,
	                procTimeSampler, surrogateInterArrivalTimeSampler, jobWeightSampler);

	        return surrogate;
	    }

	    @Override
	    public Simulation4Ind surrogateBusy(int numWorkCenters, int numJobsRecorded,
	                                int warmupJobs) {
	        double utilLevel = 1;
	        int surrogateMaxNumOps = maxNumOps;
	        AbstractIntegerSampler surrogateNumOpsSampler = numOpsSampler.clone();
	        AbstractRealSampler surrogateInterArrivalTimeSampler = interArrivalTimeSampler.clone();
	        if (surrogateMaxNumOps > numWorkCenters) {
	            surrogateMaxNumOps = numWorkCenters;
	            surrogateNumOpsSampler.setUpper(surrogateMaxNumOps);

	            surrogateInterArrivalTimeSampler.setMean(interArrivalTimeMean(numWorkCenters,
	                    minNumOps, surrogateMaxNumOps, utilLevel));
	        }

	        Simulation4Ind surrogate = new DynamicSimulation4Ind(seed, individual, numWorkCenters,
	                numJobsRecorded, warmupJobs, minNumOps, surrogateMaxNumOps,
	                utilLevel, dueDateFactor, revisit, surrogateNumOpsSampler,
	                procTimeSampler, surrogateInterArrivalTimeSampler, jobWeightSampler);

	        return surrogate;
	    }

	    public static DynamicSimulation4Ind standardFull(
	            long seed,
	            CpxGPIndividual indi,
	            int numWorkCenters,
	            int numJobsRecorded,
	            int warmupJobs,
	            double utilLevel,
	            double dueDateFactor) {
	        return new DynamicSimulation4Ind(seed, indi, numWorkCenters, numJobsRecorded,
	                warmupJobs, numWorkCenters, numWorkCenters, utilLevel,
	                dueDateFactor, false);
	    }

	    public static DynamicSimulation4Ind standardMissing(
	            long seed,
	            CpxGPIndividual indi,
	            int numWorkCenters,
	            int numJobsRecorded,
	            int warmupJobs,
	            double utilLevel,
	            double dueDateFactor) {
	        return new DynamicSimulation4Ind(seed, indi, numWorkCenters, numJobsRecorded,
	                warmupJobs, 2, numWorkCenters, utilLevel, dueDateFactor, false);
	    }
}
