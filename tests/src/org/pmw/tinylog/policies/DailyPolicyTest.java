/*
 * Copyright 2012 Martin Winandy
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.pmw.tinylog.policies;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.pmw.tinylog.util.FileHelper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for daily policy.
 * 
 * @see DailyPolicy
 */
public class DailyPolicyTest extends AbstractTimeBasedTest {

	/**
	 * Test rolling at midnight by default constructor.
	 */
	@Test
	public final void testDefaultRollingAtMidnight() {
		Policy policy = new DailyPolicy();
		assertTrue(policy.check(null, null));
		increaseTime(DAY - 1L); // 23:59:59,999
		assertTrue(policy.check(null, null));
		increaseTime(1L); // 24:00
		assertFalse(policy.check(null, null));
	}

	/**
	 * Test rolling at midnight by setting explicitly 24:00.
	 */
	@Test
	public final void testRollingAtMidnight() {
		setTime(DAY / 2L); // 12:00

		Policy policy = new DailyPolicy(24, 0);
		assertTrue(policy.check(null, null));
		increaseTime(DAY / 2 - 1L); // 23:59:59,999
		assertTrue(policy.check(null, null));
		increaseTime(1L); // 24:00
		assertFalse(policy.check(null, null));

		policy.reset();
		assertTrue(policy.check(null, null));
		increaseTime(DAY - 1L); // 23:59:59,999
		assertTrue(policy.check(null, null));
		increaseTime(1L); // 24:00
		assertFalse(policy.check(null, null));
	}

	/**
	 * Test exception for hour = -1.
	 */
	@Test(expected = IllegalArgumentException.class)
	public final void testIllegalHour() {
		new DailyPolicy(-1, 0);
	}

	/**
	 * Test exception for minute = -1.
	 */
	@Test(expected = IllegalArgumentException.class)
	public final void testNegativeMinute() {
		new DailyPolicy(12, -1);
	}

	/**
	 * Test exception for minute = 60.
	 */
	@Test(expected = IllegalArgumentException.class)
	public final void testTooHighMinute() {
		new DailyPolicy(12, 60);
	}

	/**
	 * Test String parameter for "24:00".
	 */
	@Test
	public final void testStringParameterForMidnight() {
		Policy policy = new DailyPolicy("24:00");
		assertTrue(policy.check(null, null));
		increaseTime(DAY - 1L); // 23:59:59,999
		assertTrue(policy.check(null, null));
		increaseTime(1L); // 24:00
		assertFalse(policy.check(null, null));
	}

	/**
	 * Test String parameter for "12".
	 */
	@Test
	public final void testStringParameterForTwelveAM() {
		Policy policy = new DailyPolicy("12");
		assertTrue(policy.check(null, null));
		increaseTime(HOUR * 12 - 1L); // 11:59:59,999
		assertTrue(policy.check(null, null));
		increaseTime(1L); // 12:00
		assertFalse(policy.check(null, null));
	}

	/**
	 * Test String parameter for "09:30".
	 */
	@Test
	public final void testStringParameterForHalfPastNine() {
		Policy policy = new DailyPolicy("09:30");
		assertTrue(policy.check(null, null));
		increaseTime(HOUR * 9 + MINUTE * 30 - 1L); // 09:29:59,999
		assertTrue(policy.check(null, null));
		increaseTime(1L); // 09:30
		assertFalse(policy.check(null, null));
	}

	/**
	 * Test String parameter for invalid hour ("AB:30").
	 */
	@Test(expected = IllegalArgumentException.class)
	public final void testStringParameterForInvalidHour() {
		new DailyPolicy("AB:30");
	}

	/**
	 * Test String parameter for invalid minute ("09:AB").
	 */
	@Test(expected = IllegalArgumentException.class)
	public final void testStringParameterForInvalidMinute() {
		new DailyPolicy("09:AB");
	}

	/**
	 * Test continuing log files.
	 * 
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testContinueLogFile() throws IOException {
		setTime(DAY / 2L); // 12:00
		File file = FileHelper.createTemporaryFile(null);
		file.setLastModified(getTime());

		Policy policy = new DailyPolicy();
		assertTrue(policy.initCheck(file));
		assertTrue(policy.check(null, null));
		increaseTime(DAY / 2L - 1L); // 23:59:59,999
		assertTrue(policy.check(null, null));
		increaseTime(1L); // 24:00
		assertFalse(policy.check(null, null));

		increaseTime(-1L); // 23:59:59,999
		policy = new DailyPolicy();
		assertTrue(policy.initCheck(file));
		assertTrue(policy.check(null, null));
		increaseTime(1L); // 24:00
		assertFalse(policy.check(null, null));
	}

	/**
	 * Test discontinuing log files.
	 * 
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testDiscontinueLogFile() throws IOException {
		setTime(DAY / 2L); // 12:00
		File file = FileHelper.createTemporaryFile(null);
		file.setLastModified(getTime());

		assertTrue(new DailyPolicy().initCheck(file));
		increaseTime(DAY); // Next day 12:00
		assertFalse(new DailyPolicy().initCheck(file));

		file.delete();
	}

	/**
	 * Test non-existing log files.
	 * 
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testNonExistingLogFile() throws IOException {
		File file = FileHelper.createTemporaryFile(null);
		file.delete();

		Policy policy = new DailyPolicy();
		assertTrue(policy.initCheck(file));
	}

}