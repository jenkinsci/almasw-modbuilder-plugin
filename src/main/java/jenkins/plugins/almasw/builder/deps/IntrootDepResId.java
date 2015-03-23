package jenkins.plugins.almasw.builder.deps;

import hudson.Extension;
import hudson.ExtensionPoint;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;

public class IntrootDepResId extends IntrootDepRes {
	
	protected final int id;
	
	@DataBoundConstructor
	public IntrootDepResId(int id) { 
		this.id = id;
	}
	
	@Exported
	public int getId() {
		return id;
	}
	
	public String getJenkinsId() {
		return String.valueOf(this.getJenkinsId());
	}
	
	@Extension(ordinal = 10095)
	public static class IntrootDepResIdDescriptor extends IntrootDepResDescriptor {
		
		@Override
		public String getDisplayName() {
			return EnumBuild.jobId.name();
		}
	}
}
