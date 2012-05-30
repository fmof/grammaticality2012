package edu.jhu.coe.tokenizer;

import java.io.IOException;
import java.util.List;

public interface LineTokenizer {
	public List<String> tokenizeLine(String line) throws IOException;
}
