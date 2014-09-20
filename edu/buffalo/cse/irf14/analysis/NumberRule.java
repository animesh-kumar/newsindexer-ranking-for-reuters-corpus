package edu.buffalo.cse.irf14.analysis;

import edu.buffalo.cse.irf14.common.RegExp;
import edu.buffalo.cse.irf14.common.StringUtil;

public class NumberRule extends TokenFilter {

	public NumberRule(TokenStream stream) {
		super(stream);
		this.stream = stream;
	}

	/**
	 * Function that applies rule to the current token
	 */
	public void applyFilter() {
		Token token = stream.getCurrent();
		String termText = token.getTermText();
		// if the token is a real number delete the token
		if (StringUtil.matchRegex(termText, RegExp.REGEX_REAL_NUM)) {
			stream.remove();
			stream.next();
		}
		// if the token is a composite number (i.e. fractions or percentages
		// ,replace
		// only the digits
		else if (StringUtil.matchRegex(termText, RegExp.REGEX_COMPOSITE_NUM)) {
			termText = termText.replaceAll(RegExp.REGEX_NUM_PERIOD, "");
		}
		token.setTermText(termText);
	}

}
