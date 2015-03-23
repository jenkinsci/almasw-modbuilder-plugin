package jenkins.plugins.almasw.builder.deps;

import hudson.Extension;
import hudson.ExtensionPoint;

import org.kohsuke.stapler.DataBoundConstructor;

public class IntrootDepResCompleted extends IntrootDepRes {

	@DataBoundConstructor
	public IntrootDepResCompleted() { }
	
	public String getJenkinsId() {
		return EnumBuild.lastCompletedBuild.name();
	}
	
	@Extension(ordinal = 10098)
	public static class IntrootDepResCompletedDescriptor extends IntrootDepResDescriptor {

		@Override
		public String getDisplayName() {
			return EnumBuild.lastCompletedBuild.name();
		}
	}
}
