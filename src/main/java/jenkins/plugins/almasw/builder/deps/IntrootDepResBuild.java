package jenkins.plugins.almasw.builder.deps;

import hudson.Extension;
import hudson.ExtensionPoint;

import org.kohsuke.stapler.DataBoundConstructor;

public class IntrootDepResBuild extends IntrootDepRes {
	
	@DataBoundConstructor
	public IntrootDepResBuild() { }
	
	public String getJenkinsId() {
		return EnumBuild.lastBuild.name();
	}
	
	@Extension(ordinal = 10100)
	public static class IntrootDepResBuildDescriptor extends IntrootDepResDescriptor {
		
		@Override
		public String getDisplayName() {
			return EnumBuild.lastBuild.name();
		}
	}

}
