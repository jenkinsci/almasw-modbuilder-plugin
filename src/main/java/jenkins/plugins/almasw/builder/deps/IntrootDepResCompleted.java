package jenkins.plugins.almasw.builder.deps;

import hudson.Extension;
import hudson.ExtensionPoint;

import org.kohsuke.stapler.DataBoundConstructor;

public class IntrootDepResCompleted extends IntrootDepRes implements ExtensionPoint {

	@DataBoundConstructor
	public IntrootDepResCompleted() { }
	
	@Extension(ordinal = 10098)
	public static class IntrootDepResCompletedDescriptor extends IntrootDepResDescriptor {

		@Override
		public String getDisplayName() {
			return EnumBuild.lastCompletedBuild.name();
		}
	}
}
