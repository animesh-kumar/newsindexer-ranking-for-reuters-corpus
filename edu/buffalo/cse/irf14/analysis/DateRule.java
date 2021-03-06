package edu.buffalo.cse.irf14.analysis;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.buffalo.cse.irf14.common.CommonConstants;
import edu.buffalo.cse.irf14.common.Month;
import edu.buffalo.cse.irf14.common.RegExp;

/**
 * @author animeshk
 *
 */
public class DateRule extends TokenFilter {

	// private ArrayList<String> punctuationList = null;

	// Indicates whether a string passed is a valid month
	private boolean validMonth = false;
	// Indicates that the next token is an year prefix or not.
	private boolean validYearPrefix = false;

	private String punctuations = "";

	public DateRule(TokenStream stream) {
		super(stream);
		this.stream = stream;
	}

	@Override
	public void applyFilter() {
		handleDateAndTime(stream);

	}

	private String formatYearWithBCADinSameToken(String termText) {

		String year = "1900", yearPrefix = "";
		// Check if term matches a year with BC or AD as suffix, with or
		// without punctuations
		Matcher yearGroup = CommonConstants.PATTERN_FOR_YEAR_BC_AD
				.matcher(termText);
		if (yearGroup.matches()) {
			year = yearGroup.group(1);
			yearPrefix = yearGroup.group(2);
			punctuations = yearGroup.group(3);
			year = String.format("%04d", Integer.parseInt(year));
			if (yearPrefix.equals("BC")) {
				year = "-" + year;
			}
		}
		return year;

	}

	private String formatYearWithoutBCAD(String termText) {
		String year = "1900";
		Matcher yearGroup = CommonConstants.PATTERN_FOR_YEAR.matcher(termText);
		if (yearGroup.matches()) {
			year = yearGroup.group(1);
			punctuations = yearGroup.group(2);
			year = String.format("%04d", Integer.parseInt(year));
		}
		return year;
	}

	/**
	 * Should be called after formatYearWithoutBCAD() only and never else.
	 * 
	 * @param termText
	 * @return
	 */
	private String formatYearBasedOnNextToken(String termText) {
		String yearPrefix = "";
		validYearPrefix = false;
		// Check if next Token consists of BC or AD
		if (termText != null
				&& (termText.matches(RegExp.REGEX_BC_AD
						+ RegExp.REGEX_EXT_PUNCTUATION))) {
			Matcher yearPrefixGroup = CommonConstants.PATTERN_FOR_YEAR_PREFIX
					.matcher(termText);
			if (yearPrefixGroup.matches()) {
				yearPrefix = yearPrefixGroup.group(1);
				validYearPrefix = true;
				if (yearPrefix.equals("BC")) {
					yearPrefix = "-";
				} else {
					yearPrefix = "";
				}
				punctuations = yearPrefixGroup.group(2);
			}
		}
		return yearPrefix;
	}

	private String formatMonth(String termText) {
		String month = "00";
		validMonth = false;
		Matcher monthGroup = CommonConstants.PATTERN_FOR_MONTH
				.matcher(termText);
		if (monthGroup.matches()) {
			month = monthGroup.group(1);
			punctuations = monthGroup.group(2);
			month = Month.valueOfDesc(month);
			if (month != null) {
				validMonth = true;
			}
		}
		return month;
	}

	private String formatDate(String termText) {
		String date = "00";
		// Check if is a date with or without punction
		Matcher dateGroup = CommonConstants.PATTERN_FOR_DATE.matcher(termText);
		if (dateGroup.matches()) {
			date = dateGroup.group(1);
			punctuations = dateGroup.group(2);
			date = String.format("%02d", Integer.parseInt(date));
		}
		return date;
	}

	private String handleDateAndTime(TokenStream stream) {
		// Check for Date, Month, Year or Date, Month
		// Get the first token
		Token firstToken = stream.getCurrent();
		Token secondToken = null, thirdToken = null, fourthToken = null;
		String firstTermText = null, secondTermText = null, thirdTermText = null, fourthTermText = null;
		String monthValue = "01", dateValue = "01", yearValue = "1900";
		String year = "", yearPrefix = "";
		String formattedDateValue = yearValue + monthValue + dateValue
				+ punctuations;
		// Handle Case 1 : Eg 01 January 1900 or 31 Jan 2000 or 28 Feb or 02
		// August 90
		if (firstToken != null) {
			firstTermText = firstToken.getTermText();
			// Assuming that First Token being a date won't be sufficient alone
			// and hence can't have any punctuations. So match it in entirety
			// Check if the first token is a date i.e., 1-31
			if (Character.isDigit(firstTermText.charAt(0))
					&& (firstTermText.matches(RegExp.REGEX_DATE))) {
				dateValue = formatDate(firstTermText);
				// Valid Date Value found. Now Check for a month
				// Get the next token
				secondToken = stream.next();
				if (secondToken != null) {
					secondTermText = secondToken.getTermText();
					// Second Term being a month can have a punctuation. Eg. 01
					// January!!!!, 28 Feb, 2014
					if (secondTermText.matches(RegExp.REGEX_MONTHS
							+ RegExp.REGEX_EXT_PUNCTUATION)) {
						monthValue = formatMonth(secondTermText);
						if (validMonth) {
							// Remove the month token
							stream.remove();
							// Check if the punctuation ends the sentence. If it
							// does, don't proceed
							if (punctuations.matches(RegExp.REGEX_SENT_ENDS)) {
								firstToken.setTermText(yearValue + monthValue
										+ dateValue + punctuations);
								firstToken.setDatetime(true);
								return firstToken.getTermText();
							}
							// It may or maynot be followed by an year
							thirdToken = stream.next();
							if (thirdToken != null) {
								thirdTermText = thirdToken.getTermText();
								// Check if term matches a normal year with or
								// without punctuations
								if (thirdTermText
										.matches(RegExp.REGEX_FULL_YEAR_BC_AD
												+ RegExp.REGEX_EXT_PUNCTUATION)) {
									yearValue = formatYearWithBCADinSameToken(thirdTermText);
									// Remove the year token
									stream.remove();
									// Update the first token by the
									// correct date value
									firstToken.setTermText(yearValue
											+ monthValue + dateValue
											+ punctuations);
									firstToken.setDatetime(true);
									return yearValue + monthValue + dateValue
											+ punctuations;
								} else if (thirdTermText
										.matches(RegExp.REGEX_FULL_YEAR
												+ RegExp.REGEX_EXT_PUNCTUATION)) {
									// Get the fourth token to check whether
									// it's BC
									// or AD
									yearValue = formatYearWithoutBCAD(thirdTermText);
									// Remove the year token
									stream.remove();
									// Check if the punctuation ends the
									// sentence. If it
									// does, don't proceed
									if (punctuations
											.matches(RegExp.REGEX_SENT_ENDS)) {
										firstToken.setTermText(yearValue
												+ monthValue + dateValue
												+ punctuations);
										firstToken.setDatetime(true);
										return firstToken.getTermText();
									}
									fourthToken = stream.next();
									if (fourthToken != null) {
										fourthTermText = fourthToken
												.getTermText();
										yearPrefix = formatYearBasedOnNextToken(fourthTermText);
										yearValue = yearPrefix + yearValue;
										if (validYearPrefix) {
											stream.remove();
										}

									}
									// Update the first token by the
									// correct date value
									firstToken.setTermText(yearValue
											+ monthValue + dateValue
											+ punctuations);
									firstToken.setDatetime(true);
									return yearValue + monthValue + dateValue
											+ punctuations;
								} else {
									// If the value was not a number, default it
									// to
									// 1900
									yearValue = "1900";
									firstToken.setTermText(yearValue
											+ monthValue + dateValue
											+ punctuations);
									firstToken.setDatetime(true);
									return yearValue + monthValue + dateValue
											+ punctuations;
								}
							} else {
								// Set the date value
								firstToken.setTermText(yearValue + monthValue
										+ dateValue + punctuations);
								firstToken.setDatetime(true);
							}
						}
					}
				}
			}
			// The first Token might be a month too, Eg. Jan 7 2014 or Feb 2010
			if (Character.isLetter(firstTermText.charAt(0))
					&& (firstTermText.matches(RegExp.REGEX_MONTHS
							+ RegExp.REGEX_EXT_PUNCTUATION))) {
				monthValue = formatMonth(firstTermText);
				if (validMonth) {
					yearValue = "1900";
					dateValue = "01";
					// Punctuation after month, don't go search further
					if (punctuations.matches(RegExp.REGEX_SENT_ENDS)) {
						return firstToken.getTermText();
					}
					// Now check for Date or Year
					// Get the next token
					secondToken = stream.next();
					if (secondToken != null) {
						secondTermText = secondToken.getTermText();
						// Check if is a date with or without punction
						if (secondTermText.matches(RegExp.REGEX_DATE
								+ RegExp.REGEX_EXT_PUNCTUATION)) {
							dateValue = formatDate(secondTermText);
							stream.remove();
							// Check if the punctuation ends the sentence. If it
							// does, don't proceed
							if (punctuations.matches(RegExp.REGEX_SENT_ENDS)) {
								firstToken.setTermText(yearValue + monthValue
										+ dateValue + punctuations);
								firstToken.setDatetime(true);
								return firstToken.getTermText();
							}
							// Find the third token and check if it's an
							// year else default it to 1900
							thirdToken = stream.next();
							if (thirdToken != null) {
								thirdTermText = thirdToken.getTermText();
								// Check if term matches a normal year with or
								// without punctuations
								if (thirdTermText
										.matches(RegExp.REGEX_FULL_YEAR_BC_AD
												+ RegExp.REGEX_EXT_PUNCTUATION)) {
									yearValue = formatYearWithBCADinSameToken(thirdTermText);
									// Remove the year token
									stream.remove();
									// Update the first token by the
									// correct date value
									firstToken.setTermText(yearValue
											+ monthValue + dateValue
											+ punctuations);
									firstToken.setDatetime(true);
									return yearValue + monthValue + dateValue
											+ punctuations;
								} else if (thirdTermText
										.matches(RegExp.REGEX_FULL_YEAR
												+ RegExp.REGEX_EXT_PUNCTUATION)) {
									// Get the fourth token to check whether
									// it's BC
									// or AD
									yearValue = formatYearWithoutBCAD(thirdTermText);
									// Remove the year token
									stream.remove();
									// Check if the punctuation ends the
									// sentence. If it
									// does, don't proceed
									if (punctuations
											.matches(RegExp.REGEX_SENT_ENDS)) {
										firstToken.setTermText(yearValue
												+ monthValue + dateValue
												+ punctuations);
										firstToken.setDatetime(true);
										return firstToken.getTermText();
									}
									fourthToken = stream.next();
									if (fourthToken != null) {
										fourthTermText = fourthToken
												.getTermText();
										yearPrefix = formatYearBasedOnNextToken(fourthTermText);
										yearValue = yearPrefix + yearValue;
										if (validYearPrefix) {
											stream.remove();
										}

									}
									// Update the first token by the
									// correct date value
									firstToken.setTermText(yearValue
											+ monthValue + dateValue
											+ punctuations);
									firstToken.setDatetime(true);
									return yearValue + monthValue + dateValue
											+ punctuations;
								} else {
									// If the value was not a number, default it
									// to
									// 1900
									yearValue = "1900";
									firstToken.setTermText(yearValue
											+ monthValue + dateValue
											+ punctuations);
									firstToken.setDatetime(true);
									return yearValue + monthValue + dateValue
											+ punctuations;
								}
							} else {
								// If the value was not a number,
								// default it to 1900
								yearValue = "1900";
								firstToken.setTermText(yearValue + monthValue
										+ dateValue + punctuations);
								firstToken.setDatetime(true);
							}
						} else if (secondTermText
								.matches(RegExp.REGEX_FULL_YEAR_BC_AD
										+ RegExp.REGEX_EXT_PUNCTUATION)) {
							yearValue = formatYearWithBCADinSameToken(secondTermText);
							// We don't expect the date to come after the Year,
							// hence default it to 01
							dateValue = "01";
							// Remove the year token
							stream.remove();
							// Update the first token by the
							// correct date value
							firstToken.setTermText(yearValue + monthValue
									+ dateValue + punctuations);
							firstToken.setDatetime(true);
							return yearValue + monthValue + dateValue
									+ punctuations;
						} else if (secondTermText
								.matches(RegExp.REGEX_FULL_YEAR
										+ RegExp.REGEX_EXT_PUNCTUATION)) {
							yearValue = formatYearWithoutBCAD(secondTermText);
							// Remove the year token
							stream.remove();
							// Check if the punctuation ends the sentence. If it
							// does, don't proceed
							if (punctuations.matches(RegExp.REGEX_SENT_ENDS)) {
								firstToken.setTermText(yearValue + monthValue
										+ dateValue + punctuations);
								firstToken.setDatetime(true);
								return firstToken.getTermText();
							}
							// Get the third token to check whether
							// it's BC or AD
							thirdToken = stream.next();
							if (thirdToken != null) {
								thirdTermText = thirdToken.getTermText();
								yearPrefix = formatYearBasedOnNextToken(thirdTermText);
								yearValue = yearPrefix + yearValue;
								if (validYearPrefix) {
									stream.remove();
								}
							}
							// Update the first token by the
							// correct date value
							firstToken.setTermText(yearValue + monthValue
									+ dateValue + punctuations);
							firstToken.setDatetime(true);
							return yearValue + monthValue + dateValue
									+ punctuations;
						} else if (secondTermText.matches(RegExp.REGEX_DATE
								+ "[-/]" + RegExp.REGEX_DATE
								+ RegExp.REGEX_EXT_PUNCTUATION)) {
							// For dates like May 25-28 or May
							String[] splits = secondTermText.split("[-/]");
							if (splits.length == 2) {
								String date1 = formatDate(splits[0]);
								String date2 = formatDate(splits[1]);
								stream.remove();
								yearValue = "1900";
								firstToken.setTermText(yearValue + monthValue
										+ date1 + "-" + yearValue + monthValue
										+ date2 + punctuations);
								firstToken.setDatetime(true);
								// Check if the punctuation ends the sentence.
								// If it does, don't proceed
								if (punctuations
										.matches(RegExp.REGEX_SENT_ENDS)) {
									return firstToken.getTermText();

								} else {
									thirdToken = stream.next();
									if (thirdToken != null) {
										thirdTermText = thirdToken
												.getTermText();
										if (thirdTermText
												.matches(RegExp.REGEX_FULL_YEAR
														+ RegExp.REGEX_EXT_PUNCTUATION)) {
											yearValue = formatYearWithoutBCAD(thirdTermText);
											// Remove the year token
											stream.remove();
											firstToken.setTermText(yearValue
													+ monthValue + date1 + "-"
													+ yearValue + monthValue
													+ date2 + punctuations);
											return firstToken.getTermText();
										}

									}

								}
							}
						}

					}
				}
			}

			// The firsToken might be an year too . 2014.
			if (Character.isDigit(firstTermText.charAt(0))
					&& (firstTermText.matches(RegExp.REGEX_YEAR
							+ RegExp.REGEX_EXT_PUNCTUATION))) {
				yearValue = formatYearWithoutBCAD(firstTermText);
				// If less than 4 digits, number alone is not sufficient for
				// year
				// Check if next token is BC or AD
				// Get the next token
				secondToken = stream.next();
				if (secondToken != null) {
					secondTermText = secondToken.getTermText();
					if (secondTermText.equals("BC")) {
						yearValue = "-"
								+ String.format("%04d",
										Integer.parseInt(firstTermText));
						stream.remove();
						formattedDateValue = yearValue + monthValue + dateValue
								+ punctuations;
						// Set the date to first token
						firstToken.setTermText(formattedDateValue);
						firstToken.setDatetime(true);
					} else if (secondTermText.equals("AD")) {
						yearValue = String.format("%04d",
								Integer.parseInt(firstTermText));
						stream.remove();
						formattedDateValue = yearValue + monthValue + dateValue
								+ punctuations;
						// Set the date to first token
						firstToken.setTermText(formattedDateValue);
						firstToken.setDatetime(true);
					} else {
						// If it was just an year i.e., No BC or AD and NO
						// Month or Date, Check if it's four digit to mark
						// it as date
						if (firstTermText.matches("\\d{4}")
								&& (Integer.parseInt(firstTermText) >= 1900)
								&& !secondTermText
										.matches(RegExp.REGEX_TIME_AM_PM)) {
							yearValue = String.format("%04d",
									Integer.parseInt(firstTermText));
							formattedDateValue = yearValue + monthValue
									+ dateValue + punctuations;
							// Set the date to first token
							firstToken.setTermText(formattedDateValue);
							firstToken.setDatetime(true);
							// Date formatted. No need of further
							// processing.
							return yearValue + monthValue + dateValue
									+ punctuations;
						}
					}
				} else {
					// If it was just an year i.e., No BC or AD and NO
					// Month or Date, Check if it's four digit to mark
					// it as date
					if (firstTermText.matches("\\d{4}")
							&& (secondTermText == null || !secondTermText
									.matches(RegExp.REGEX_TIME_AM_PM))) {
						yearValue = String.format("%04d",
								Integer.parseInt(firstTermText));
						formattedDateValue = yearValue + monthValue + dateValue
								+ punctuations;
						// Set the date to first token
						firstToken.setTermText(formattedDateValue);
						firstToken.setDatetime(true);
						// Date formatted. No need of further
						// processing.
						return yearValue + monthValue + dateValue
								+ punctuations;
					}
				}

			}

			if (Character.isDigit(firstTermText.charAt(0))
					&& (firstTermText.matches(RegExp.REGEX_YEAR_BC_AD
							+ RegExp.REGEX_EXT_PUNCTUATION))) {

				Matcher yearGroup = CommonConstants.PATTERN_FOR_YEAR_BC_AD
						.matcher(firstTermText);
				if (yearGroup.matches()) {
					year = String.format("%04d",
							Integer.parseInt(yearGroup.group(1)));
					yearPrefix = yearGroup.group(2);
					punctuations = yearGroup.group(3);
					// Check if BC or AD exists
					if (yearPrefix.toUpperCase().endsWith("BC")) {
						yearValue = "-" + year;
					} else if (yearPrefix.toUpperCase().endsWith("AD")) {
						yearValue = year;
					}

					formattedDateValue = yearValue + monthValue + dateValue
							+ punctuations;
					// Set the date to first token
					firstToken.setTermText(formattedDateValue);
					firstToken.setDatetime(true);
				}
			}
			// If the date is separated by a hyphen
			// 2011-12 or 867BC-876AD or
			if (Character.isDigit(firstTermText.charAt(0))
					&& (firstTermText.matches(RegExp.REGEX_COMPOSITE_YEAR
							+ RegExp.REGEX_EXT_PUNCTUATION))) {
				// Split the token into constituent dates and recurse through
				// this method
				String[] splitDates = firstTermText.split("[-/]");
				punctuations = "";
				if (splitDates.length == 2) {
					String year1 = formatYearWithoutBCAD(splitDates[0]);
					// Since the part two will be only two digits, add first two
					// digits for split[0]
					String year2 = formatYearWithoutBCAD(splitDates[0]
							.substring(0, 2) + splitDates[1]);
					firstToken.setTermText(year1 + monthValue + dateValue + "-"
							+ year2 + monthValue + dateValue + punctuations);
					firstToken.setDatetime(true);
				}
			}

			String hours = "00", minutes = "00", seconds = "00";
			// Handling Time formats which might or mightnot have AM/PM in the
			// same token
			String regexForTime = RegExp.REGEX_HOURS
					+ RegExp.REGEX_TIME_SEPARATOR
					+ RegExp.REGEX_MINUTES_SECONDS
					+ RegExp.REGEX_TIME_SEPARATOR
					+ RegExp.REGEX_MINUTES_SECONDS + RegExp.REGEX_TIME_AM_PM
					+ RegExp.REGEX_EXT_PUNCTUATION;
			if (Character.isDigit(firstTermText.charAt(0))
					&& firstTermText.matches(regexForTime)) {
				Pattern timePattern = Pattern.compile(regexForTime);
				Matcher timeGroup = timePattern.matcher(firstTermText);
				if (timeGroup.matches()) {
					// Take out the punctuation already
					punctuations = timeGroup.group(7);
					hours = timeGroup.group(1);
					if (!hours.isEmpty()) {
						hours = String.format("%02d", Integer.parseInt(hours));
					} else {
						// No hours means no time
						return null;
					}
					// If minutes is present, next two groups will have the same
					// value because of the pattern grouping else, next group
					// will be empty whereas the group next will be null
					minutes = timeGroup.group(3);
					if (!minutes.isEmpty()) {
						minutes = String.format("%02d",
								Integer.parseInt(minutes));
					} else {
						minutes = "00";
					}
					// Group 4 will hold the smaller group of pattern grouping
					// Group 5 will hold the separator
					seconds = timeGroup.group(6);
					if (!seconds.isEmpty()) {
						seconds = String.format("%02d",
								Integer.parseInt(seconds));
					} else {
						seconds = "00";
					}
					// Group 7 is not usable
					// Group 8 holds te separator
					// Check if AM/PM exists in the same token Group 9 and 10
					String ampm = timeGroup.group(8);
					if (!ampm.isEmpty()) {
						// If PM , Add 12 to the hours otherwise return as it is
						if (ampm.equalsIgnoreCase("PM")) {
							int updatedhour = 12 + Integer.parseInt(hours);
							hours = String.format("%02d", updatedhour);
						}
						// Due to grouping group 9 is a wasted
						// group. Punctuations are retrieved from
						// group 10
						punctuations = timeGroup.group(10);
						firstToken.setTermText(hours + ":" + minutes + ":"
								+ seconds + punctuations);
						firstToken.setDatetime(true);
					} else {
						// Check if next token has AM PM
						secondToken = stream.next();
						if (secondToken != null) {
							secondTermText = secondToken.getTermText();
							String regexAmPm = RegExp.REGEX_TIME_AM_PM
									+ RegExp.REGEX_EXT_PUNCTUATION;
							if (secondTermText.matches(regexAmPm)) {
								Pattern amPmPattern = Pattern
										.compile(regexAmPm);
								Matcher amPmGroup = amPmPattern
										.matcher(secondTermText);
								if (amPmGroup.matches()) {
									ampm = amPmGroup.group(1);
									// If PM , Add 12 to the hours otherwise
									// return as it is
									if (ampm.equalsIgnoreCase("PM")) {
										int updatedhour = 12 + Integer
												.parseInt(hours);
										hours = String.format("%02d",
												updatedhour);
									}
									// Due to grouping group 2 is a wasted
									// group. Punctuations are retrieved from
									// group 3
									punctuations = amPmGroup.group(3);
									stream.remove();
									firstToken.setTermText(hours + ":"
											+ minutes + ":" + seconds
											+ punctuations);
									firstToken.setDatetime(true);
								}
							}
						}
					}
				}

			}
		}
		return null;
	}
}
