package com.jlibrosa.audio.process;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

/**
 * This Class calculates the MFCC, STFT values of given audio samples.
 * 
 * This source code has been referenced from Open Source
 * 
 * @author abhi-rawat1
 *
 */
public class AudioFeatureExtraction {

	private static int n_mfcc = 40;
	private static double sampleRate = 44100.0;

	private final static double fMax = sampleRate / 2.0;
	private final static double fMin = 0.0;
	private final static int n_fft = 2048;
	private final static int hop_length = 512;
	private final static int n_mels = 128;

	/**
	 * Variable for holding Sample Rate value
	 * 
	 * @param sampleRateVal
	 */
	public void setSampleRate(double sampleRateVal) {
		sampleRate = sampleRateVal;
	}

	/**
	 * Variable for holding n_mfcc value
	 * 
	 * @param n_mfccVal
	 */
	public void setN_mfcc(int n_mfccVal) {
		n_mfcc = n_mfccVal;
	}

	/**
	 * This function extract MFCC values from given Audio Magnitude Values.
	 * 
	 * @param doubleInputBuffer
	 * @return
	 */
	public float[] extractMFCCFeatures(double[] doubleInputBuffer) {
		final double[][] mfccResult = dctMfcc(doubleInputBuffer);
		return finalshape(mfccResult);
	}

	/**
	 * This function converts 2D MFCC values into 1d
	 * 
	 * @param mfccSpecTro
	 * @return
	 */
	private float[] finalshape(double[][] mfccSpecTro) {
		float[] finalMfcc = new float[mfccSpecTro[0].length * mfccSpecTro.length];
		int k = 0;
		for (int i = 0; i < mfccSpecTro[0].length; i++) {
			for (int j = 0; j < mfccSpecTro.length; j++) {
				finalMfcc[k] = (float) mfccSpecTro[j][i];
				k = k + 1;
			}
		}
		return finalMfcc;
	}

	/**
	 * This function converts DCT values into mfcc
	 * 
	 * @param y
	 * @return
	 */
	private double[][] dctMfcc(double[] y) {
		final double[][] specTroGram = powerToDb(melSpectrogram(y));
		final double[][] dctBasis = dctFilter(n_mfcc, n_mels);
		double[][] mfccSpecTro = new double[n_mfcc][specTroGram[0].length];
		for (int i = 0; i < n_mfcc; i++) {
			for (int j = 0; j < specTroGram[0].length; j++) {
				for (int k = 0; k < specTroGram.length; k++) {
					mfccSpecTro[i][j] += dctBasis[i][k] * specTroGram[k][j];
				}
			}
		}
		return mfccSpecTro;
	}

	/**
	 * This function generates mel spectrogram values
	 * 
	 * @param y
	 * @return
	 */
	private double[][] melSpectrogram(double[] y) {
		double[][] melBasis = melFilter();
		double[][] spectro = extractSTFTFeatures(y);
		double[][] melS = new double[melBasis.length][spectro[0].length];
		for (int i = 0; i < melBasis.length; i++) {
			for (int j = 0; j < spectro[0].length; j++) {
				for (int k = 0; k < melBasis[0].length; k++) {
					melS[i][j] += melBasis[i][k] * spectro[k][j];
				}
			}
		}
		return melS;
	}

	/**
	 * This function extract STFT values from given Audio Magnitude Values.
	 * 
	 * @param y
	 * @return
	 */
	public double[][] extractSTFTFeatures(double[] y) {
		// Short-time Fourier transform (STFT)
		final double[] fftwin = getWindow();
		// pad y with reflect mode so it's centered. This reflect padding implementation
		// is
		double[] ypad = new double[n_fft + y.length];
		for (int i = 0; i < n_fft / 2; i++) {
			ypad[(n_fft / 2) - i - 1] = y[i + 1];
			ypad[(n_fft / 2) + y.length + i] = y[y.length - 2 - i];
		}
		for (int j = 0; j < y.length; j++) {
			ypad[(n_fft / 2) + j] = y[j];
		}

		final double[][] frame = yFrame(ypad);
		double[][] fftmagSpec = new double[1 + n_fft / 2][frame[0].length];

		double[] fftFrame = new double[n_fft];

		for (int k = 0; k < frame[0].length; k++) {
			int fftFrameCounter = 0;
			for (int l = 0; l < n_fft; l++) {
				fftFrame[fftFrameCounter] = fftwin[l] * frame[l][k];
				fftFrameCounter = fftFrameCounter + 1;
			}

			double[] tempConversion = new double[fftFrame.length];
			double[] tempImag = new double[fftFrame.length];

			FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
			try {
				Complex[] complx = transformer.transform(fftFrame, TransformType.FORWARD);

				for (int i = 0; i < complx.length; i++) {
					double rr = (complx[i].getReal());

					double ri = (complx[i].getImaginary());

					tempConversion[i] = rr;
					tempImag[i] = ri;
				}

			} catch (IllegalArgumentException e) {
				System.out.println(e);
			}

			double[] magSpec = tempConversion;
			for (int i = 0; i < 1 + n_fft / 2; i++) {
				fftmagSpec[i][k] = magSpec[i];
			}
		}
		return fftmagSpec;
	}

	/**
	 * This function is used to get hann window, librosa
	 * 
	 * @return
	 */
	private double[] getWindow() {
		// Return a Hann window for even n_fft.
		// The Hann window is a taper formed by using a raised cosine or sine-squared
		// with ends that touch zero.
		double[] win = new double[n_fft];
		for (int i = 0; i < n_fft; i++) {
			win[i] = 0.5 - 0.5 * Math.cos(2.0 * Math.PI * i / n_fft);
		}
		return win;
	}

	/**
	 * This function is used to apply padding and return Frame
	 * 
	 * @param ypad
	 * @return
	 */
	private double[][] yFrame(double[] ypad) {
		final int n_frames = 1 + (ypad.length - n_fft) / hop_length;
		double[][] winFrames = new double[n_fft][n_frames];
		for (int i = 0; i < n_fft; i++) {
			for (int j = 0; j < n_frames; j++) {
				winFrames[i][j] = ypad[j * hop_length + i];
			}
		}
		return winFrames;
	}

	/**
	 * This function is used to convert Power Spectrogram values into db values.
	 * 
	 * @param melS
	 * @return
	 */
	private double[][] powerToDb(double[][] melS) {
		// Convert a power spectrogram (amplitude squared) to decibel (dB) units
		// This computes the scaling ``10 * log10(S / ref)`` in a numerically
		// stable way.
		double[][] log_spec = new double[melS.length][melS[0].length];
		double maxValue = -100;
		for (int i = 0; i < melS.length; i++) {
			for (int j = 0; j < melS[0].length; j++) {
				double magnitude = Math.abs(melS[i][j]);
				if (magnitude > 1e-10) {
					log_spec[i][j] = 10.0 * log10(magnitude);
				} else {
					log_spec[i][j] = 10.0 * (-10);
				}
				if (log_spec[i][j] > maxValue) {
					maxValue = log_spec[i][j];
				}
			}
		}

		// set top_db to 80.0
		for (int i = 0; i < melS.length; i++) {
			for (int j = 0; j < melS[0].length; j++) {
				if (log_spec[i][j] < maxValue - 80.0) {
					log_spec[i][j] = maxValue - 80.0;
				}
			}
		}
		// ref is disabled, maybe later.
		return log_spec;
	}

	/**
	 * This function is used to get dct filters.
	 * 
	 * @param n_filters
	 * @param n_input
	 * @return
	 */
	private double[][] dctFilter(int n_filters, int n_input) {
		// Discrete cosine transform (DCT type-III) basis.
		double[][] basis = new double[n_filters][n_input];
		double[] samples = new double[n_input];
		for (int i = 0; i < n_input; i++) {
			samples[i] = (1 + 2 * i) * Math.PI / (2.0 * (n_input));
		}
		for (int j = 0; j < n_input; j++) {
			basis[0][j] = 1.0 / Math.sqrt(n_input);
		}
		for (int i = 1; i < n_filters; i++) {
			for (int j = 0; j < n_input; j++) {
				basis[i][j] = Math.cos(i * samples[j]) * Math.sqrt(2.0 / (n_input));
			}
		}
		return basis;
	}

	/**
	 * This function is used to create a Filterbank matrix to combine FFT bins into
	 * Mel-frequency bins.
	 * 
	 * @return
	 */
	private double[][] melFilter() {
		// Create a Filterbank matrix to combine FFT bins into Mel-frequency bins.
		// Center freqs of each FFT bin
		final double[] fftFreqs = fftFreq();
		// 'Center freqs' of mel bands - uniformly spaced between limits
		final double[] melF = melFreq(n_mels + 2);

		double[] fdiff = new double[melF.length - 1];
		for (int i = 0; i < melF.length - 1; i++) {
			fdiff[i] = melF[i + 1] - melF[i];
		}

		double[][] ramps = new double[melF.length][fftFreqs.length];
		for (int i = 0; i < melF.length; i++) {
			for (int j = 0; j < fftFreqs.length; j++) {
				ramps[i][j] = melF[i] - fftFreqs[j];
			}
		}

		double[][] weights = new double[n_mels][1 + n_fft / 2];
		for (int i = 0; i < n_mels; i++) {
			for (int j = 0; j < fftFreqs.length; j++) {
				double lowerF = -ramps[i][j] / fdiff[i];
				double upperF = ramps[i + 2][j] / fdiff[i + 1];
				if (lowerF > upperF && upperF > 0) {
					weights[i][j] = upperF;
				} else if (lowerF > upperF && upperF < 0) {
					weights[i][j] = 0;
				} else if (lowerF < upperF && lowerF > 0) {
					weights[i][j] = lowerF;
				} else if (lowerF < upperF && lowerF < 0) {
					weights[i][j] = 0;
				} else {
				}
			}
		}

		double enorm[] = new double[n_mels];
		for (int i = 0; i < n_mels; i++) {
			enorm[i] = 2.0 / (melF[i + 2] - melF[i]);
			for (int j = 0; j < fftFreqs.length; j++) {
				weights[i][j] *= enorm[i];
			}
		}
		return weights;

		// need to check if there's an empty channel somewhere
	}

	/**
	 * To get fft frequencies
	 * 
	 * @return
	 */
	private double[] fftFreq() {
		// Alternative implementation of np.fft.fftfreqs
		double[] freqs = new double[1 + n_fft / 2];
		for (int i = 0; i < 1 + n_fft / 2; i++) {
			freqs[i] = 0 + (sampleRate / 2) / (n_fft / 2) * i;
		}
		return freqs;
	}

	/**
	 * To get mel frequencies
	 * 
	 * @param numMels
	 * @return
	 */
	private double[] melFreq(int numMels) {
		// 'Center freqs' of mel bands - uniformly spaced between limits
		double[] LowFFreq = new double[1];
		double[] HighFFreq = new double[1];
		LowFFreq[0] = fMin;
		HighFFreq[0] = fMax;
		final double[] melFLow = freqToMel(LowFFreq);
		final double[] melFHigh = freqToMel(HighFFreq);
		double[] mels = new double[numMels];
		for (int i = 0; i < numMels; i++) {
			mels[i] = melFLow[0] + (melFHigh[0] - melFLow[0]) / (numMels - 1) * i;
		}
		return melToFreq(mels);
	}

	/**
	 * To convert mel frequencies into hz frequencies
	 * 
	 * @param mels
	 * @return
	 */
	private double[] melToFreqS(double[] mels) {
		double[] freqs = new double[mels.length];
		for (int i = 0; i < mels.length; i++) {
			freqs[i] = 700.0 * (Math.pow(10, mels[i] / 2595.0) - 1.0);
		}
		return freqs;
	}

	/**
	 * To convert hz frequencies into mel frequencies.
	 * 
	 * @param freqs
	 * @return
	 */
	protected double[] freqToMelS(double[] freqs) {
		double[] mels = new double[freqs.length];
		for (int i = 0; i < freqs.length; i++) {
			mels[i] = 2595.0 * log10(1.0 + freqs[i] / 700.0);
		}
		return mels;
	}

	/**
	 * To convert mel frequencies into hz frequencies
	 * 
	 * @param mels
	 * @return
	 */
	private double[] melToFreq(double[] mels) {
		// Fill in the linear scale
		final double f_min = 0.0;
		final double f_sp = 200.0 / 3;
		double[] freqs = new double[mels.length];

		// And now the nonlinear scale
		final double min_log_hz = 1000.0; // beginning of log region (Hz)
		final double min_log_mel = (min_log_hz - f_min) / f_sp; // same (Mels)
		final double logstep = Math.log(6.4) / 27.0;

		for (int i = 0; i < mels.length; i++) {
			if (mels[i] < min_log_mel) {
				freqs[i] = f_min + f_sp * mels[i];
			} else {
				freqs[i] = min_log_hz * Math.exp(logstep * (mels[i] - min_log_mel));
			}
		}
		return freqs;
	}

	/**
	 * To convert hz frequencies into mel frequencies
	 * 
	 * @param freqs
	 * @return
	 */
	protected double[] freqToMel(double[] freqs) {
		final double f_min = 0.0;
		final double f_sp = 200.0 / 3;
		double[] mels = new double[freqs.length];

		// Fill in the log-scale part

		final double min_log_hz = 1000.0; // beginning of log region (Hz)
		final double min_log_mel = (min_log_hz - f_min) / f_sp; // # same (Mels)
		final double logstep = Math.log(6.4) / 27.0; // step size for log region

		for (int i = 0; i < freqs.length; i++) {
			if (freqs[i] < min_log_hz) {
				mels[i] = (freqs[i] - f_min) / f_sp;
			} else {
				mels[i] = min_log_mel + Math.log(freqs[i] / min_log_hz) / logstep;
			}
		}
		return mels;
	}

	/**
	 * To get log10 value.
	 * 
	 * @param value
	 * @return
	 */
	private double log10(double value) {
		return Math.log(value) / Math.log(10);
	}
}
