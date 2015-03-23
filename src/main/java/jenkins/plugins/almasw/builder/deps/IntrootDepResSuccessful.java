package jenkins.plugins.almasw.builder.deps;

import hudson.Extension;
import hudson.ExtensionPoint;

import org.kohsuke.stapler.DataBoundConstructor;

public class IntrootDepResSuccessful extends IntrootDepRes {

	@DataBoundConstructor
	public IntrootDepResSuccessful() { }
	
	public String getJenkinsId() {
		return EnumBuild.lastSuccessfulBuild.name();
	}
	
	@Extension(ordinal = 10099)
	public static class IntrootDepResSuccessfulDescriptor extends IntrootDepResDescriptor {

		public String getDisplayName() {
			return EnumBuild.lastSuccessfulBuild.name();
		}
	}
}
