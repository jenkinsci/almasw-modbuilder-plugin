package jenkins.plugins.almasw.builder.deps;

import hudson.Extension;
import hudson.ExtensionPoint;

import org.kohsuke.stapler.DataBoundConstructor;

public class IntrootDepResUnstable extends IntrootDepRes {

	@DataBoundConstructor
	public IntrootDepResUnstable() { }

	@Extension(ordinal = 10098)
	public static class IntrootDepResUnstableDescriptor extends IntrootDepResDescriptor {

		public String getDisplayName() {
			return EnumBuild.lastUnstableBuild.name();
		}
	}
}
