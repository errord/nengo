/*
 * Created on 23-Apr-07
 */
package ca.neo.model.nef.impl;

import ca.neo.math.Function;
import ca.neo.math.PDF;
import ca.neo.math.impl.AbstractFunction;
import ca.neo.math.impl.ConstantFunction;
import ca.neo.math.impl.GradientDescentApproximator;
import ca.neo.math.impl.IndicatorPDF;
import ca.neo.model.Configuration;
import ca.neo.model.impl.ConfigurationImpl;
import ca.neo.model.Node;
import ca.neo.model.StructuralException;
import ca.neo.model.nef.NEFEnsemble;
import ca.neo.model.nef.NEFEnsembleFactory;
import ca.neo.model.neuron.Neuron;
import ca.neo.model.neuron.impl.LIFNeuronFactory;
import ca.neo.util.MU;
import ca.neo.util.VectorGenerator;
import ca.neo.util.impl.RandomHypersphereVG;
import ca.neo.util.impl.Rectifier;

/**
 * <p>Part of a projection in which each of the Nodes making up an Ensemble is a source of only excitatory or inhibitory 
 * connections.</p> 
 * 
 * <p>The theory is presented in Parisien, Anderson & Eliasmith (2007).</p>
 *  
 * <p>Such a projection includes a "base" DecodedOrigin and DecodedTermination (a projection between these may have weights
 * of mixed sign). The projection is expanded with a BiasOrigin a pair of BiasTerminations, and a new NEFEnsemble of 
 * interneurons. The make weight signs uniform, a projection is established between the BiasOrigin and BiasTermination, 
 * in parallel with the original projection. The effective synaptic weights that arise from the combination of these two 
 * projections are of uniform sign. However, the post-synaptic Ensemble receives extra bias current as a result. This bias 
 * current is cancelled by a projection from the BiasOrigin through the interneurons, to a second BiasTermination.</p> 
 *    
 * TODO: account for transformations in the Termination, which can change sign and magnitude of weights
 * 
 * @author Bryan Tripp
 */
public class BiasOrigin extends DecodedOrigin {
	
	private static final long serialVersionUID = 1L;		
	
	private NEFEnsemble myInterneurons;
	private float[][] myConstantOutputs;
	
	public BiasOrigin(Node node, String name, Node[] nodes, String nodeOrigin, float[][] constantOutputs, int numInterneurons, boolean excitatory) throws StructuralException {
		super(node, name, nodes, nodeOrigin, new Function[]{new ConstantFunction(1, 0f)}, getUniformBiasDecoders(constantOutputs, excitatory));		
		
		myInterneurons = createInterneurons(name + ":interneurons", numInterneurons, excitatory);
		myConstantOutputs = constantOutputs;
	}
	
	/**
	 * This method adjusts bias decoders so that the bias function is as flat as possible, without changing the 
	 * bias encoders on the post-synaptic ensemble. Distortion can be minimized by calling this method and then 
	 * calling optimizeInterneuronDomain().   
	 * 
	 * @param baseWeights Matrix of synaptic weights in the unbiased projection (ie the weights of mixed sign)  
	 * @param biasEncoders Encoders of the bias dimension on the post-synaptic ensemble 
	 */
	public void optimizeDecoders(float[][] baseWeights, float[] biasEncoders) {
		float[][] evalPoints = MU.transpose(new float[][]{new float[myConstantOutputs[0].length]}); //can use anything here because target function is constant
		GradientDescentApproximator.Constraints constraints = new BiasEncodersMaintained(baseWeights, biasEncoders);
		GradientDescentApproximator approximator = new GradientDescentApproximator(evalPoints, MU.clone(myConstantOutputs), constraints, true);
		approximator.setStartingCoefficients(MU.transpose(getDecoders())[0]);
		float[] newDecoders = approximator.findCoefficients(new ConstantFunction(1, 0));
		super.setDecoders(MU.transpose(new float[][]{newDecoders}));
	}
	
	/**
	 * This method adjusts the interneuron channel so that the interneurons are tuned to the 
	 * range of values that is output by the bias function.  
	 * 
	 * @param interneuronTermination The Termination on getInterneurons() that recieves input from this Origin 
	 * @param biasTermination The BiasTermination to which the interneurons project (not the one to which this Origin 
	 * 		projects directly)
	 */
	public void optimizeInterneuronDomain(DecodedTermination interneuronTermination, DecodedTermination biasTermination) {
		float[] range = this.getRange();
		range[0] = range[0] - .25f * (range[1] - range[0]); //avoid distorted area near zero in interneurons 
		interneuronTermination.setStaticBias(new float[]{-range[0]});
		interneuronTermination.getTransform()[0][0] = 1f / (range[1] - range[0]);
		biasTermination.setStaticBias(new float[]{range[0]/(range[1] - range[0])});
		biasTermination.getTransform()[0][0] = -(range[1] - range[0]);				
	}
	
	/**
	 * @return Vector of mininum and maximum output of this origin, ie {min, max}
	 */
	public float[] getRange() {
		float[] outputs = MU.prod(MU.transpose(myConstantOutputs), MU.transpose(getDecoders())[0]);		
		return new float[]{MU.min(outputs), MU.max(outputs)};
	}
	
	private static float[][] getUniformBiasDecoders(float[][] constantOutputs, boolean excitatory) {
		float[][] result = new float[constantOutputs.length][];
		float decoder = getBiasDecoder(constantOutputs, excitatory);
		for (int i = 0; i < result.length; i++) {
			result[i] = new float[]{decoder};
		}
		return result;
	}
	
	private static float getBiasDecoder(float[][] constantOutputs, boolean excitatory) {
		//iterate over evaluation points to find max of sum(constantOutputs)
		float max = 0;
		for (int i = 0; i < constantOutputs[0].length; i++) { 
			float sum = 0;
			for (int j = 0; j < constantOutputs.length; j++) {
				sum += constantOutputs[j][i];
			}
			if (sum > max) max = sum;
		}
		
		return excitatory ? 1f / max : -1f / max; //this makes the bias function peak at 1 (or -1) 
	}
	
	private NEFEnsemble createInterneurons(String name, int num, boolean excitatoryProjection) throws StructuralException {
		NEFEnsembleFactory ef = null;
		if (excitatoryProjection) {
			ef = new NEFEnsembleFactoryImpl();
		} else {
			ef = new NEFEnsembleFactoryImpl() {
				protected void addDefaultOrigins(NEFEnsemble ensemble) throws StructuralException {
					Function f = new AbstractFunction(1) {
						private static final long serialVersionUID = 1L;
						public float map(float[] from) {
							return -1 - from[0];
						}
					};
					ensemble.addDecodedOrigin(NEFEnsemble.X, new Function[]{f}, Neuron.AXON);
				}
			};
		}
		//TODO: handle additional bias in inhibitory case 
		
		new NEFEnsembleFactoryImpl();
		ef.setEncoderFactory(new Rectifier(ef.getEncoderFactory(), true));
		ef.setEvalPointFactory(new BiasedVG(new RandomHypersphereVG(false, 0.5f, 0f), 0, excitatoryProjection ? .5f : -.5f));
		
//		PDF interceptPDF = excitatoryProjection ? new IndicatorPDF(-.5f, .75f) : new IndicatorPDF(-.99f, .35f);
		PDF interceptPDF = excitatoryProjection ? new IndicatorPDF(-.15f, .5f) : new IndicatorPDF(-1.2f, .1f); //was -.5f, .75f for excitatory
		PDF maxRatePDF = excitatoryProjection ? new IndicatorPDF(200f, 500f) : new IndicatorPDF(400f, 800f);
		ef.setNodeFactory(new LIFNeuronFactory(.02f, .0001f, maxRatePDF, interceptPDF));
		ef.setApproximatorFactory(new GradientDescentApproximator.Factory(new CoefficientsSameSign(excitatoryProjection), false)); 
		
		return ef.make(name, num, 1);
	}

	/**
	 * @return An ensemble of interneurons through which this Origin must project (in parallel with its 
	 * 		direct projection) to compensate for the bias introduced by making all weights the same sign.  
	 */
	public NEFEnsemble getInterneurons() {
		return myInterneurons;
	}

	/**
	 * Forces all decoding coefficients to be >= 0. 
	 * 
	 * @author Bryan Tripp
	 */
	public static class CoefficientsSameSign implements GradientDescentApproximator.Constraints {
		
		private static final long serialVersionUID = 1L;
		
		private boolean mySignPositive;
		
		public CoefficientsSameSign(boolean positive) {
			mySignPositive = positive;
		}
		
		/**
		 * @see ca.neo.math.impl.GradientDescentApproximator.Constraints#correct(float[])
		 */
		public boolean correct(float[] coefficients) {
			boolean allCorrected = true;
			for (int i = 0; i < coefficients.length; i++) {
				if ( (mySignPositive && coefficients[i] < 0) || (!mySignPositive && coefficients[i] > 0)) {
					coefficients[i] = 0;
				} else {
					allCorrected = false;
				}
			}
			return allCorrected;
		}
	}
	
	private static class BiasEncodersMaintained implements GradientDescentApproximator.Constraints {

		private static final long serialVersionUID = 1L;
		
		private float[][] myBaseWeights;
		private float[] myBiasEncoders; 
		
		public BiasEncodersMaintained(float[][] baseWeights, float[] biasEncoders) {
			myBaseWeights = baseWeights;
			myBiasEncoders = biasEncoders;
		}
		
		public boolean correct(float[] coefficients) {
			boolean allCorrected = true;
			
			for (int i = 0; i < coefficients.length; i++) {
				boolean corrected = false;

				if (coefficients[i] < 0) { //next correction will fail
					coefficients[i] = Float.MIN_VALUE; 
					corrected = true;
				}
				
				for (int j = 0; j < myBiasEncoders.length; j++) {
					if ( - myBaseWeights[j][i] / coefficients[i] > myBiasEncoders[j] )  {
						coefficients[i] = - myBaseWeights[j][i] / myBiasEncoders[j];
						corrected = true;
					}
				}
				
				if (!corrected) allCorrected = false;
			}
			
			return allCorrected;
		}
		
	}

	/**
	 * Adds a specified bias to a specified dimension of vectors that are made by an underlying generator. 
	 *  
	 * @author Bryan Tripp
	 */
	private static class BiasedVG implements VectorGenerator {
		
		private VectorGenerator myVG;
		private int myDim;
		private float myBias;
		private Configuration myConfiguration;
		
		public BiasedVG(VectorGenerator vg, int dim, float bias) {
			myVG = vg;
			myDim = dim;
			myBias = bias;
			
			// this can be empty because it's never configured (only contructed locally)
			myConfiguration = new ConfigurationImpl(this); 
		}

		/**
		 * @see ca.neo.model.Configurable#getConfiguration()
		 */
		public Configuration getConfiguration() {
			return myConfiguration;
		}

		/**
		 * @see ca.neo.util.VectorGenerator#genVectors(int, int)
		 */
		public float[][] genVectors(int number, int dimension) {
			float[][] result = myVG.genVectors(number, dimension);
			for (int i = 0; i < result.length; i++) {
				result[i][myDim] += myBias;
			}
			return result;
		}
	}

}
