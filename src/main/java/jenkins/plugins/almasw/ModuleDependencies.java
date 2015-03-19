package jenkins.plugins.almasw;

import java.io.Serializable;
import java.util.Collection;

import jenkins.model.Jenkins;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.Exported;

public class ModuleDependencies extends
		AbstractDescribableImpl<ModuleDependencies> implements Serializable {

	private static final long serialVersionUID = 1L;

	private final String project;
	private final String build;
	private final String strategy;
	private final String location;
	private final int jobId;

	@DataBoundConstructor
	public ModuleDependencies(String project, String build, String strategy,
			String location, int jobId, String introot) {
		
		this.project = project;
		this.build = build;
		this.strategy = strategy;
		this.location = location;
		this.jobId = jobId;
	}

	@Exported
	public String getProject() {
		return project;
	}

	@Exported
	public String getBuild() {
		return build;
	}

	@Exported
	public String getStrategy() {
		return strategy;
	}

	@Exported
	public String getLocation() {
		return location;
	}
	
	@Exported
	public int getJobId() {
		return jobId;
	}
	public String toString() {
		StringBuilder sbuilder = new StringBuilder();
		
		sbuilder.append(" depends on ");
		sbuilder.append(this.getProject());
		sbuilder.append(" job, using ");
		sbuilder.append(this.getLocation());
		sbuilder.append(" from ");
		
		if(EnumBuild.valueOf(this.getBuild()) == EnumBuild.jobId) {
			sbuilder.append(this.getJobId());
			sbuilder.append(" build ");
		} else {
			sbuilder.append("the ");
			sbuilder.append(EnumBuild.valueOf(this.getBuild()).getReadable());
			sbuilder.append(" ");
		}
		
		sbuilder.append(this.getStrategy());
		
		return sbuilder.toString();
	}

	@Extension
	public static final class DescriptorImpl extends Descriptor<ModuleDependencies> {
		
		@Override
		public String getDisplayName() {
			return "";
		}
		
		// validations
		
		public ListBoxModel doFillBuildItems() {
			ListBoxModel items = new ListBoxModel();
			for (EnumBuild eBuild : EnumBuild.values())
				items.add(eBuild.getReadable(), eBuild.name());
			return items;
		}

		public ListBoxModel doFillStrategyItems() {
			ListBoxModel items = new ListBoxModel();
			for (EnumStrategy eStrategy : EnumStrategy.values())
				items.add(eStrategy.name(), eStrategy.name());
			return items;
		}
		
		public AutoCompletionCandidates doAutoCompleteProject(
				@QueryParameter String value) {
			AutoCompletionCandidates projects = new AutoCompletionCandidates();
//			Jenkins.getInstance().getProjects().get(0).getBuildByNumber(1).getArtifactsDir();
//			Jenkins.getInstance().getProjects().get(0).getBuildsAsMap();
			for (String job : Jenkins.getInstance().getJobNames())
				projects.add(job);
			return projects;
		}
	}
}
