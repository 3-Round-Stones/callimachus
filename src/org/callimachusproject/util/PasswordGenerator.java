/*
 * Copyright (c) 2010, James Leigh Some rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.callimachusproject.util;

import java.security.SecureRandom;
import java.util.Random;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * Generates a random string of printable characters that can be used as a
 * password.
 * 
 * @author James Leigh
 * 
 */
public class PasswordGenerator {

	public static void main(String[] args) {
		System.out.println(PasswordGenerator.generatePassword());
	}

	public static String generatePassword() {
		return PasswordGenerator.getInstance().nextPassword();
	}

	public static PasswordGenerator getInstance() {
		return instance;
	}

	private static final PasswordGenerator instance = new PasswordGenerator();

	private static long byteToLong(byte[] seed) {
		long hash = 1;
		for (byte b : seed) {
			hash = hash * 31 + b;
		}
		if (hash == 0)
			return 1;
		return hash;
	}

	private final Random rnd;

	public PasswordGenerator() {
		rnd = new SecureRandom();
	}

	public PasswordGenerator(long seed) {
		rnd = new Random(seed);
	}

	public PasswordGenerator(byte[] seed) {
		this(byteToLong(DigestUtils.sha256(seed)));
	}

	/**
	 * This algorithm selects from a word pool of 1916 words. This gives an
	 * entropy of 11 bits per word. The average word length is 6. The average
	 * entropy of each password generated is 55 bits.
	 * 
	 * The entropy of a 8 character password of random [A-Za-z0-9] is 48 bits.
	 * 
	 * @return a random pass-phrase
	 */
	public String nextPassword() {
		int length = LENGTH_MIN + rnd.nextInt(LENGTH_MAX - LENGTH_MIN + 1);
		StringBuilder sb = new StringBuilder();
		while (sb.length() < length) {
			if (sb.length() > 0) {
				sb.append('-');
			}
			sb.append(WORD_POOL[rnd.nextInt(WORD_POOL.length)]);
		}
		return sb.toString();
	}

	private static final int LENGTH_MIN = 25;
	private static final int LENGTH_MAX = 35;
	private static final String[] WORD_POOL = { "a", "ability", "able",
		"about", "above", "academic", "accept", "accepted", "according",
		"account", "achieved", "achievement", "across", "act", "acting",
		"action", "actions", "active", "activities", "activity", "actual",
		"actually", "add", "added", "addition", "additional", "address",
		"adequate", "administration", "advance", "advantage", "affairs",
		"afraid", "after", "afternoon", "again", "against", "age",
		"agencies", "agency", "ago", "agreed", "agreement", "ahead", "aid",
		"air", "aircraft", "alive", "all", "allow", "allowed", "almost",
		"alone", "along", "already", "also", "although", "always", "am",
		"America", "among", "amount", "an", "analysis", "ancient", "and",
		"animal", "animals", "announced", "annual", "anode", "another",
		"answer", "answered", "any", "anyone", "anything", "apart",
		"apartment", "apparent", "apparently", "appeal", "appear",
		"appearance", "appeared", "appears", "application", "applied",
		"apply", "approach", "appropriate", "approximately", "April",
		"are", "area", "areas", "argument", "arm", "armed", "arms", "army",
		"around", "arrived", "art", "article", "artist", "artists", "arts",
		"as", "aside", "ask", "asked", "asking", "aspects", "assignment",
		"assistance", "Associated", "association", "assume", "assumed",
		"at", "atmosphere", "attack", "attempt", "attention", "attitude",
		"attorney", "audience", "authority", "available", "average",
		"avoid", "aware", "away", "baby", "back", "background", "balance",
		"ball", "bank", "bar", "base", "baseball", "based", "basic",
		"basis", "battle", "bay", "be", "beach", "bear", "beat",
		"beautiful", "beauty", "became", "because", "become", "becomes",
		"becoming", "bed", "been", "before", "began", "begin", "beginning",
		"begins", "behavior", "behind", "being", "belief", "believe",
		"believed", "below", "beneath", "benefit", "Berlin", "beside",
		"besides", "best", "better", "between", "beyond", "Bible", "big",
		"bill", "billion", "birds", "birth", "bit", "block", "blood",
		"blue", "board", "boat", "bodies", "body", "book", "books", "born",
		"Boston", "both", "bottle", "bottom", "bought", "box", "break",
		"bridge", "brief", "bright", "bring", "Britain", "British",
		"broad", "broke", "broken", "brother", "brought", "brown",
		"budget", "build", "building", "buildings", "built", "business",
		"busy", "but", "buy", "by", "California", "call", "called",
		"calls", "came", "camp", "campaign", "can", "cannot", "capable",
		"capacity", "capital", "captain", "car", "care", "career",
		"careful", "carefully", "carried", "carry", "carrying", "cars",
		"case", "cases", "cattle", "caught", "cause", "caused", "causes",
		"cell", "cells", "cent", "center", "central", "century", "certain",
		"certainly", "chair", "chairman", "chance", "change", "changed",
		"changes", "Chapter", "character", "characteristic", "charge",
		"charged", "Charles", "check", "chemical", "Chicago", "chief",
		"China", "choice", "chosen", "circle", "circumstances", "cities",
		"citizens", "city", "civil", "claim", "claims", "class", "classes",
		"Clay", "clean", "clear", "clearly", "close", "closed", "closely",
		"closer", "clothes", "club", "coast", "coffee", "cold",
		"collection", "college", "color", "column", "combination", "come",
		"comes", "coming", "command", "commerce", "commercial",
		"Commission", "committee", "common", "communication", "community",
		"companies", "Company", "compared", "competition", "complete",
		"completed", "completely", "completion", "complex", "components",
		"concept", "concern", "concerned", "concerning", "conclusion",
		"condition", "conditions", "conduct", "conducted", "conference",
		"confidence", "Congo", "Congress", "connection", "consider",
		"considerable", "considered", "constant", "construction",
		"contact", "contained", "contemporary", "continue", "continued",
		"continuing", "contract", "contrast", "control", "cool", "corner",
		"corporation", "Corps", "cost", "costs", "could", "council",
		"countries", "country", "county", "couple", "course", "courses",
		"court", "cover", "covered", "created", "credit", "critical",
		"cross", "cultural", "culture", "current", "cut", "cutting",
		"daily", "Dallas", "dance", "danger", "dark", "data", "date",
		"daughter", "day", "days", "dead", "deal", "death", "December",
		"decided", "decision", "declared", "deep", "defense", "degree",
		"demand", "demands", "Democratic", "department", "described",
		"design", "designed", "desire", "desk", "despite", "detail",
		"details", "determine", "determined", "develop", "developed",
		"development", "device", "dictionary", "did", "die", "died",
		"difference", "differences", "different", "difficult",
		"difficulty", "dinner", "direct", "directed", "direction",
		"directly", "director", "discovered", "discussed", "discussion",
		"distance", "distribution", "District", "divided", "division",
		"do", "doctor", "does", "dog", "dogs", "doing", "dollars",
		"domestic", "dominant", "done", "door", "double", "doubt", "down",
		"Dr", "dramatic", "draw", "drawn", "dream", "dress", "drew",
		"drink", "drive", "drop", "dropped", "drove", "dry", "due",
		"during", "dust", "duty", "each", "earlier", "early", "earth",
		"easily", "east", "easy", "eat", "economic", "economy", "edge",
		"editor", "education", "educational", "effect", "effective",
		"effects", "effort", "efforts", "eight", "either", "election",
		"electric", "electronic", "elements", "else", "emphasis",
		"employees", "empty", "end", "ended", "ends", "enemy", "energy",
		"England", "English", "enjoyed", "enough", "enter", "entered",
		"entire", "entirely", "entitled", "entrance", "equal", "equally",
		"equipment", "escape", "especially", "essential", "establish",
		"established", "estimated", "etc", "Europe", "even", "evening",
		"event", "events", "ever", "every", "everybody", "everyone",
		"everything", "evidence", "evident", "exactly", "example",
		"excellent", "except", "exchange", "executive", "exercise",
		"exist", "existence", "existing", "expect", "expected",
		"experience", "experiment", "experiments", "explain", "explained",
		"expressed", "expression", "extended", "extent", "extreme", "eye",
		"eyes", "face", "faces", "facilities", "fact", "factor", "factors",
		"facts", "faculty", "failed", "failure", "fair", "fairly", "fall",
		"familiar", "families", "family", "famous", "far", "farm",
		"fashion", "fast", "father", "favor", "fear", "features",
		"federal", "feed", "feel", "feeling", "feelings", "feet", "fell",
		"fellow", "felt", "few", "field", "fields", "fifteen", "fifty",
		"fig", "fight", "fighting", "figure", "figures", "file", "filled",
		"film", "final", "finally", "financial", "find", "finds", " fine",
		"fingers", "finished", "fire", "firm", "firms", "first", "fiscal",
		"fit", "five", "fixed", "flat", "floor", "flow", "flowers",
		"follow", "followed", "following", "follows", "food", "foot",
		"for", "force", "forced", "forces", "foreign", "forest", "form",
		"formed", "former", "forms", "formula", "fort", "forth", "forward",
		"found", "Four", "fourth", "frame", "France", "Frank", "free",
		"freedom", "frequently", "Fresh", "Friday", "friend", "friendly",
		"friends", "from", "front", "full", "fully", "function", "fund",
		"funds", "further", "future", "gain", "game", "games", "garden",
		"gas", "gave", "general", "generally", "generation", "George",
		"Germany", "get", "gets", "getting", "give", "given", "gives",
		"giving", "Glass", "go", "goal", "goes", "going", "gone", "good",
		"goods", "got", "government", "governments", "governor", "granted",
		"gray", "great", "greater", "greatest", "greatly", "Green", "grew",
		"gross", "ground", "grounds", "group", "groups", "grow", "growing",
		"growth", "guess", "guests", "gun", "had", "hair", "half", "hall",
		"hand", "hands", "Hanover", "happen", "happened", "happy", "hard",
		"hardly", "has", "hat", "have", "having", "he", "head", "headed",
		"headquarters", "health", "hear", "heard", "hearing", "heart",
		"heat", "heavily", "heavy", "held", "hell", "help", "helped",
		"hence", "Henry", "her", "here", "herself", "high", "higher",
		"highest", "highly", "hill", "him", "himself", "his", "historical",
		"history", "hit", "hold", "holding", "hole", "home", "homes",
		"honor", "hope", "horse", "horses", "hospital", "hot", "Hotel",
		"hour", "hours", "house", "houses", "housing", "how", "however",
		"human", "hundred", "I", "idea", "ideal", "ideas", "if", "image",
		"imagination", "imagine", "immediate", "immediately", "impact",
		"importance", "important", "impossible", "improved", "in", "inch",
		"inches", "include", "included", "including", "income", "increase",
		"increased", "increases", "increasing", "indeed", "independence",
		"independent", "index", "India", "indicate", "indicated",
		"individual", "individuals", "industrial", "industry", "influence",
		"information", "informed", "initial", "inside", "instance",
		"instead", "institutions", "intellectual", "intensity", "interest",
		"interested", "interesting", "interests", "interior", "internal",
		"international", "into", "involved", "is", "island", "issue",
		"issues", "it", "items", "its", "itself", "Jack", "James", "jazz",
		"job", "jobs", "Joe", "John", "join", "joined", "Jones", "Joseph",
		"Judge", "judgment", "July", "June", "junior", "junior", "Jury",
		"just", "justice", "keep", "keeping", "Kennedy", "kept", "key",
		"Khrushchev", "kid", "kind", "king", "kitchen", "knew", "knife",
		"know", "knowledge", "known", "knows", "la", "labour", "lack",
		"Lady", "laid", "land", "language", "Laos", "large", "largely",
		"larger", "last", "late", "later", "latter", "law", "laws", "lay",
		"lead", "Leader", "leaders", "leadership", "leading", "league",
		"learn", "learned", "learning", "least", "leave", "leaving", "led",
		"left", "leg", "legal", "legs", "length", "less", "let", "letter",
		"letters", "level", "levels", "Lewis", "liberal", "library", "lie",
		"life", "light", "like", "liked", "likely", "limited", "line",
		"lines", "lips", "list", "literary", "literature", "little",
		"live", "lived", "lives", "living", "local", "located", "location",
		"London", "long", "longer", "Look", "looked", "looking", "looks",
		"Lord", "lose", "loss", "lost", "lot", "Louis", "loved", "low",
		"lower", "machine", "machinery", "made", "main", "maintain",
		"maintenance", "major", "majority", "make", "makes", "making",
		"management", "manager", "manner", "many", "March", "Mark",
		"marked", "market", "marriage", "married", "Martin", "Mary",
		"mass", "master", "material", "materials", "matter", "matters",
		"maximum", "may", "maybe", "me", "meaning", "means", "meant",
		"measure", "measured", "medical", "meet", "meeting", "member",
		"members", "membership", "memory", "mentioned", "Mercer", "merely",
		"message", "met", "Metal", "method", "methods", "Middle", "might",
		"Mike", "mile", "miles", "military", "million", "mind", "minds",
		"mine", "minimum", "Minister", "minor", "minute", "minutes",
		"Miss", "mission", "model", "modern", "moment", "Monday", "money",
		"month", "months", "moon", "moral", "more", "moreover", "Morgan",
		"morning", "most", "mother", "motion", "motor", "mouth", "move",
		"moved", "movement", "moving", "much", "Music", "musical", "must",
		"my", "myself", "name", "named", "names", "narrow", "nation",
		"national", "nations", "natural", "naturally", "nature", "near",
		"nearly", "necessary", "neck", "need", "needed", "needs", "negro",
		"negroes", "neighborhood", "neither", "never", "nevertheless",
		"new", "news", "newspaper", "next", "nice", "night", "nine", "no",
		"nobody", "none", "nor", "normal", "North", "nose", "not", "note",
		"noted", "notes", "Nothing", "notice", "novel", "November", "now",
		"nuclear", "number", "numbers", "object", "objective", "objects",
		"observed", "obtained", "obvious", "obviously", "occasion",
		"occurred", "of", "off", "offer", "offered", "office", "officer",
		"officers", "official", "officials", "often", "Oh", "oil", "on",
		"once", "one", "ones", "only", "onto", "open", "opened", "opening",
		"operating", "operation", "operations", "opinion", "opportunity",
		"opposite", "or", "orchestra", "order", "ordered", "orders",
		"Ordinary", "organization", "organizations", "organized",
		"original", "other", "others", "otherwise", "ought", "our",
		"ourselves", "out", "outside", "over", "own", "page", "paid",
		"pain", "painting", "pale", "Palmer", "paper", "parents", "Paris",
		"Park", "Parker", "part", "particular", "particularly", "parties",
		"parts", "Party", "pass", "passed", "passing", "past", "patient",
		"pattern", "pay", "peace", "people", "per", "percent", "perfect",
		"performance", "perhaps", "period", "permit", "permitted",
		"person", "personal", "personnel", "persons", "phase", "Phil",
		"philosophy", "physical", "pick", "picked", "picture", "pictures",
		"piece", "pieces", "place", "placed", "places", "plan", "plane",
		"planned", "planning", "plans", "plant", "plants", "platform",
		"play", "played", "playing", "plays", "please", "pleasure",
		"plenty", "plus", "poems", "poet", "poetry", "point", "pointed",
		"points", "police", "policies", "policy", "political", "politics",
		"pool", "popular", "population", "portion", "position", "positive",
		"possibility", "possible", "possibly", "post", "potential",
		"power", "powerful", "powers", "practical", "Practice", "prepared",
		"presence", "present", "presented", "president", "press",
		"pressure", "pretty", "prevent", "previous", "previously", "price",
		"prices", "primarily", "primary", "principal", "principle",
		"principles", "private", "probably", "problem", "problems",
		"procedure", "procedures", "process", "processes", "produce",
		"produced", "product", "production", "products", "professional",
		"professor", "program", "programs", "progress", "project",
		"projects", "proper", "properties", "property", "proposed",
		"protection", "proved", "provide", "provided", "Providence",
		"provides", "providing", "public", "published", "pulled", "pure",
		"purpose", "purposes", "put", "quality", "question", "questions",
		"quick", "quickly", "quiet", "quite", "race", "radio", "railroad",
		"rain", "raised", "ran", "range", "rapidly", "rate", "rates",
		"rather", "reach", "reached", "reaction", "read", "reading",
		"ready", "real", "reality", "realize", "realized", "really",
		"reason", "reasonable", "reasons", "receive", "received", "recent",
		"recently", "recognize", "recognized", "record", "records", "Red",
		"reduce", "reduced", "reference", "refused", "regard", "regarded",
		"region", "regular", "related", "relation", "relations",
		"relationship", "relatively", "relief", "remain", "remained",
		"remains", "remember", "remembered", "remove", "removed",
		"repeated", "replied", "report", "reported", "reports",
		"represented", "require", "required", "requirements", "requires",
		"research", "resolution", "resources", "respect", "response",
		"responsibility", "responsible", "rest", "result", "results",
		"return", "returned", "review", "revolution", "Rhode", "rich",
		"Richard", "rifle", "right", "rights", "rise", "rising", "river",
		"road", "roads", "Robert", "rock", "role", "Roman", "Rome", "roof",
		"room", "rooms", "rose", "round", "rule", "rules", "run",
		"running", "runs", "Russia", "safe", "said", "sales", "Sam",
		"same", "sample", "San", "sat", "Saturday", "save", "saw", "say",
		"saying", "says", "scale", "scene", "school", "schools", "science",
		"scientific", "score", "sea", "search", "season", "second",
		"secret", "secretary", "section", "sections", "security", "see",
		"seeing", "seek", "seem", "seemed", "seems", "seen", "selected",
		"Senate", "send", "sense", "sensitive", "sent", "separate",
		"September", "series", "serious", "serve", "served", "service",
		"services", "session", "set", "sets", "setting", "settled",
		"seven", "several", "shall", "shape", "share", "sharp", "she",
		"shelter", "ship", "shook", "shop", "shore", "short", "shot",
		"should", "shoulder", "show", "showed", "showing", "shown",
		"shows", "side", "sides", "sight", "sign", "signal",
		"significance", "significant", "signs", "similar", "simple",
		"simply", "since", "single", "Sir", "sit", "site", "sitting",
		"situation", "six", "size", "sky", "sleep", "slightly", "slow",
		"slowly", "small", "smaller", "smile", "smiled", "snow", "so",
		"social", "society", "soft", "solid", "solution", "some",
		"somebody", "somehow", "someone", "something", "sometimes",
		"somewhat", "somewhere", "son", "song", "songs", "soon", "sort",
		"sought", "sound", "source", "sources", "south", "Southern",
		"Soviet", "space", "speak", "speaking", "special", "specific",
		"speech", "speed", "spent", "spirit", "spiritual", "spite",
		"spoke", "spot", "spread", "spring", "square", "St", "staff",
		"stage", "stand", "standard", "standards", "standing", "stands",
		"stared", "start", "started", "starting", "State", "stated",
		"statement", "statements", "states", "station", "stations",
		"status", "stay", "stayed", "step", "steps", "still", "stock",
		"Stone", "stood", "stop", "stopped", "store", "stories", "story",
		"straight", "strange", "street", "streets", "strength", "stress",
		"strong", "struck", "structure", "struggle", "student", "students",
		"studied", "studies", "study", "style", "subject", "subjects",
		"substantial", "success", "successful", "such", "suddenly",
		"sufficient", "suggested", "summer", "sun", "Sunday", "supply",
		"support", "suppose", "supposed", "sure", "surface", "surprised",
		"sweet", "system", "systems", "table", "take", "taken", "takes",
		"taking", "talk", "talked", "talking", "task", "taste", "tax",
		"teacher", "teachers", "teaching", "team", "technical",
		"technique", "techniques", "teeth", "telephone", "tell",
		"temperature", "ten", "tension", "term", "terms", "test", "tests",
		"Texas", "text", "than", "that", "the", "their", "them", "theme",
		"themselves", "then", "theory", "there", "therefore", "these",
		"they", "thick", "thin", "thing", "things", "think", "thinking",
		"third", "thirty", "this", "Thomas", "those", "though", "thought",
		"thousand", "three", "through", "throughout", "thus", "time",
		"times", "title", "to", "today", "together", "told", "Tom",
		"tomorrow", "tone", "too", "took", "top", "total", "touch",
		"toward", "towards", "town", "trade", "tradition", "traditional",
		"traffic", "train", "training", "travel", "treated", "treatment",
		"tree", "trees", "trial", "tried", "trip", "trouble", "truck",
		"true", "truly", "truth", "try", "trying", "Tuesday", "turn",
		"turned", "turning", "twenty", "twice", "two", "type", "types",
		"typical", "ultimate", "UN", "uncle", "under", "understand",
		"understanding", "understood", "union", "unique", "unit", "United",
		"units", "unity", "universe", "University", "unless", "until",
		"unusual", "up", "upon", "upper", "US", "use", "used", "useful",
		"uses", "using", "usual", "usually", "valley", "value", "values",
		"variety", "various", "vast", "very", "victory", "view", "village",
		"Virginia", "vision", "visit", "vital", "vocational", "voice",
		"volume", "vote", "wage", "wait", "waited", "waiting", "walk",
		"walked", "wall", "walls", "want", "wanted", "wants", "war",
		"warm", "was", "Washington", "watch", "watched", "watching",
		"water", "way", "ways", "we", "weapons", "weather", "week",
		"weeks", "weight", "well", "went", "were", "west", "Western",
		"what", "whatever", "wheel", "when", "where", "whether", "which",
		"while", "who", "whole", "whom", "whose", "why", "wide", "wild",
		"will", "William", "willing", "Wilson", "win", "wind", "window",
		"wine", "winter", "wish", "with", "within", "without", "won",
		"wonder", "wondered", "word", "words", "wore", "work", "worked",
		"workers", "working", "works", "world", "worry", "worth", "would",
		"write", "Writer", "writers", "writing", "written", "wrong",
		"wrote", "yards", "year", "years", "Yes", "yesterday", "yet",
		"York", "you", "young", "your", "yourself", "Youth" };
}
