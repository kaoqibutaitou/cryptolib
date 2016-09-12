/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE.txt.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.cryptolib.common;

import java.security.Provider;
import java.security.SecureRandom;
import java.security.SecureRandomSpi;

/**
 * Wraps a fast CSPRNG, which gets reseeded automatically after a certain amount of bytes has been generated.<br>
 * 
 * <p>
 * Java 8 Example:
 * 
 * <pre>
 * try {
 * 	// NIST SP 800-90A Rev 1 (http://dx.doi.org/10.6028/NIST.SP.800-90Ar1) suggests 440 seed bits for up to 2^48 bytes between reseeds for SHA1/SHA2 PRNGs:
 * 	return new ReseedingSecureRandom(SecureRandom.getInstanceStrong(), SecureRandom.getInstance("SHA1PRNG"), 1 &lt;&lt; 30, 55);
 * } catch (NoSuchAlgorithmException e) {
 * 	throw new IllegalStateException("Used RNGs must exist in every Java platform.", e);
 * }
 * </pre>
 */
class ReseedingSecureRandom extends SecureRandom {

	private static final Provider PROVIDER = new ReseedingSecureRandomProvider();

	/**
	 * @param seeder RNG for high-quality random numbers. E.g. <code>SecureRandom.getInstanceStrong()</code> in Java 8 environments.
	 * @param csprng A fast csprng implementation, such as <code>SHA1PRNG</code>, that will be wrapped by this instance.
	 * @param reseedAfter How many bytes can be read from the <code>csprng</code>, before a new seed will be generated.
	 * @param seedLength Number of bytes generated by <code>seeder</code> in order to seed <code>csprng</code>.
	 */
	public ReseedingSecureRandom(SecureRandom seeder, SecureRandom csprng, long reseedAfter, int seedLength) {
		super(new ReseedingSecureRandomSpi(seeder, csprng, reseedAfter, seedLength), PROVIDER);
	}

	private static class ReseedingSecureRandomProvider extends Provider {

		protected ReseedingSecureRandomProvider() {
			super("ReseedingSecureRandomProvider", 1.0, "Provides ReseedingSecureRandom");
		}

	}

	private static class ReseedingSecureRandomSpi extends SecureRandomSpi {

		private final SecureRandom seeder, csprng;
		private final long reseedAfter;
		private final int seedLength;
		private long counter;

		public ReseedingSecureRandomSpi(SecureRandom seeder, SecureRandom csprng, long reseedAfter, int seedLength) {
			this.seeder = seeder;
			this.csprng = csprng;
			this.reseedAfter = reseedAfter;
			this.seedLength = seedLength;
			reseed();
		}

		@Override
		protected void engineSetSeed(byte[] seed) {
			csprng.setSeed(seed);
		}

		@Override
		protected void engineNextBytes(byte[] bytes) {
			if (counter + bytes.length > reseedAfter) {
				reseed();
			}
			counter += bytes.length;
			csprng.nextBytes(bytes);
		}

		@Override
		protected byte[] engineGenerateSeed(int numBytes) {
			return seeder.generateSeed(seedLength);
		}

		private void reseed() {
			engineSetSeed(engineGenerateSeed(seedLength));
			counter = 0;
		}

	}

}
