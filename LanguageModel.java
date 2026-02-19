import java.util.HashMap;
import java.util.Random;

public class LanguageModel {

    // The map of this model.
    // Maps windows to lists of charachter data objects.
    HashMap<String, List> CharDataMap;
    
    // The window length used in this model.
    int windowLength;
    
    // The random number generator used by this model. 
    private Random randomGenerator;

    /** Constructs a language model with the given window length and a given
     *  seed value. Generating texts from this model multiple times with the 
     *  same seed value will produce the same random texts. Good for debugging. */
    public LanguageModel(int windowLength, int seed) {
        this.windowLength = windowLength;
        randomGenerator = new Random(seed);
        CharDataMap = new HashMap<String, List>();
    }

    /** Constructs a language model with the given window length.
     * Generating texts from this model multiple times will produce
     * different random texts. Good for production. */
    public LanguageModel(int windowLength) {
        this.windowLength = windowLength;
        randomGenerator = new Random();
        CharDataMap = new HashMap<String, List>();
    }

    /** Builds a language model from the text in the given file (the corpus). */
    public void train(String fileName) {
        if (windowLength <= 0) return;
        StringBuilder window = new StringBuilder();
        char c;
        In in = new In(fileName);
        while (window.length() < windowLength && !in.isEmpty()) {
            window.append(in.readChar());
        }
        if (window.length() < windowLength) return;
        while (!in.isEmpty()) {
            c = in.readChar();
            String w = window.toString();
            List probs = CharDataMap.get(w);
            if (probs == null) {
                probs = new List();
                CharDataMap.put(w, probs);
            }
            probs.update(c);
            window.deleteCharAt(0);
            window.append(c);
        }
        for (List probs : CharDataMap.values()) {
            calculateProbabilities(probs);
        }
    }

    // Computes and sets the probabilities (p and cp fields) of all the
	// characters in the given list. */
    void calculateProbabilities(List probs) {				
        ListIterator itr = probs.listIterator(0);
        // count total chars
        int charsInTotal = 0;
        while (itr.hasNext()) {
            CharData cd = itr.next();
            charsInTotal += cd.count;
        }
        itr = probs.listIterator(0);
        CharData prev = null;
        CharData current = probs.getFirst();
        while (itr.hasNext()) {
            current = itr.next();
            current.p = current.count/(double)charsInTotal;
            if (prev == null) {
                current.cp = current.p;
            }
            else {
                current.cp = current.p + prev.cp;
            }
            prev = current;
        }
    }

    // Returns a random character from the given probabilities list.
    char getRandomChar(List probs) {
        double r = randomGenerator.nextDouble();
        ListIterator itr = probs.listIterator(0); while (itr.hasNext()) {
            CharData cd = itr.next();
            if (cd.cp > r) return cd.chr;
        }
        CharData last = probs.get(probs.getSize() - 1);
        return last.chr;
    }

    /**
	 * Generates a random text, based on the probabilities that were learned during training. 
	 * @param initialText - text to start with. If initialText's last substring of size numberOfLetters
	 * doesn't appear as a key in Map, we generate no text and return only the initial text. 
	 * @param numberOfLetters - the size of text to generate
	 * @return the generated text
	 */
    public String generate(String initialText, int textLength) {
        if (initialText == null || textLength <= initialText.length() || initialText.length() < windowLength) return initialText;
        StringBuilder generated = new StringBuilder(initialText);
        String window = generated.substring(generated.length() - windowLength);
        while (generated.length() < textLength) {
            List probs = CharDataMap.get(window);
            if (probs == null) break;
            char nextChar = getRandomChar(probs);
            generated.append(nextChar);
            window = generated.substring(generated.length() - windowLength);
        }
        return generated.toString();
    }

    /** Returns a string representing the map of this language model. */
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (String key : CharDataMap.keySet()) {
            List keyProbs = CharDataMap.get(key);
            str.append(key + " : " + keyProbs + "\n");
        }
        return str.toString();
    }

    public static void main(String[] args) {
        int windowLength = Integer.parseInt(args[0]);
        String initialText = args[1];
        int generatedTextLength = Integer.parseInt(args[2]);
        boolean randomGeneration = args[3].equals("random");
        String fileName = args[4];
        // create model
        LanguageModel lm;
        if (randomGeneration)
            lm = new LanguageModel(windowLength);
        else
            lm = new LanguageModel(windowLength, 20);
        // train model
        lm.train(fileName);
        // generate and print
        System.out.println(lm.generate(initialText, generatedTextLength));
    }
}
