package jenkins.plugins.almasw;

public enum EnumBuild {
	
	lastSuccessfulBuild("last sucessful"),
	lastBuild("last build"),
	lastStableBuild("last stable build"),
	lastFailedBuild("last failed build"),
	lastUnstableBuild("last unstable build"),
	lastUnsuccessfulBuild("last unsuccessful build"),
	lastCompletedBuild("last completed build"),
	jobId("Job Id");
	
	private final String readable;
	
	EnumBuild(String readable) {
		this.readable = readable;
	}

	public String getReadable() {
		return readable;
	}
}
