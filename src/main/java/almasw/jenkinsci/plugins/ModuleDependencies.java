package almasw.jenkinsci.plugins;

import java.io.Serializable;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;

	
public class ModuleDependencies extends AbstractDescribableImpl<ModuleDependencies> implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private String project;
	private String build;
	private String strategy;
	
	@DataBoundConstructor
	public ModuleDependencies(String project, String build, String strategy) {
		this.project = project;
		this.build = build;
		this.strategy = strategy;
	}
	
	@Exported
	public String geProject() {
		return project;
	}
	
	public void setProject(String project) {
		this.project = project;
	}

	@Exported
	public String getBuild() {
		return build;
	}

	public void setBuild(String build) {
		this.build = build;
	}
	
	@Exported
	public String getStrategy() {
		return strategy;
	}

	public void setStrategy(String strategy) {
		this.strategy = strategy;
	}

	@Extension
    public static final class DescriptorImpl extends Descriptor<ModuleDependencies> {

		@Override
		public String getDisplayName() {
			return "";
		}
		
	}
	
}
