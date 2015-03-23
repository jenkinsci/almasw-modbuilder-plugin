package jenkins.plugins.almasw.builder.deps;

import hudson.Extension;
import hudson.ExtensionPoint;

import org.kohsuke.stapler.DataBoundConstructor;

public class IntrootDepResFailed extends IntrootDepRes {

	@DataBoundConstructor
	public IntrootDepResFailed() { }
	
	public String getJenkinsId() {
		return EnumBuild.lastFailedBuild.name();
	}
	
	@Extension(ordinal = 10096)
	public static class IntrootDepResFailedDescriptor extends IntrootDepRes.IntrootDepResDescriptor {

		@Override
		public String getDisplayName() {
			return EnumBuild.lastFailedBuild.toString();
		}
	}
}
