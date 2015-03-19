package jenkins.plugins.almasw;

public class Temporal {
	public static void main(String args[]) {
		String method = "getRunMixIn".split("get")[1];
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		System.out.println(method.replaceAll("[a-z]*[A-Z][a-z]+", "$1_"));
	}
}
