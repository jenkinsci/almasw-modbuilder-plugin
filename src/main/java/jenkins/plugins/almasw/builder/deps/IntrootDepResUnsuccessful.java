package jenkins.plugins.almasw.builder.deps;

import hudson.Extension;
import hudson.ExtensionPoint;

import org.kohsuke.stapler.DataBoundConstructor;

public class IntrootDepResUnsuccessful extends IntrootDepRes {

	@DataBoundConstructor
	public IntrootDepResUnsuccessful() { }

	@Extension(ordinal = 10097)
	public static class IntrootDepResUnsuccessfulDescriptor extends IntrootDepResDescriptor {

		public String getDisplayName() {
			return EnumBuild.lastUnsuccessfulBuild.name();
		}
	}
}
