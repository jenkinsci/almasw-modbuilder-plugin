package jenkins.plugins.almasw.builder.deps;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jenkins.model.Jenkins;
import jenkins.plugins.almasw.builder.EnumStrategy;
import jenkins.plugins.almasw.builder.deps.IntrootDepRes.IntrootDepResDescriptor;
import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.Exported;

public class IntrootDep extends AbstractDescribableImpl<IntrootDep> implements Serializable {

	private final String project;
	private final String strategy;
	private final String location;
	private final IntrootDepRes result;

	@DataBoundConstructor
	public IntrootDep(String project, String strategy, String location, String introot, IntrootDepRes result) {
		
		this.project = project;
		this.strategy = strategy;
		this.location = location;
		this.result = result;
	}

	@Exported
	public String getProject() {
		return project;
	}
	
	@Exported
	public String getStrategy() {
		return strategy;
	}

	@Exported
	public String getLocation() {
		return location;
	}

	public IntrootDepRes getResult() {
		return result;
	}

	@Extension
	public static final class DescriptorImpl extends Descriptor<IntrootDep> {
		
		@Override
		public String getDisplayName() {
			return "";
		}
		
		// validations
		
		public ListBoxModel doFillStrategyItems() {
			ListBoxModel items = new ListBoxModel();
			for (EnumStrategy eStrategy : EnumStrategy.values())
				items.add(eStrategy.name(), eStrategy.name());
			return items;
		}
		
		public AutoCompletionCandidates doAutoCompleteProject(@QueryParameter String value) {
			AutoCompletionCandidates projects = new AutoCompletionCandidates();
			for (String job : Jenkins.getInstance().getJobNames())
				projects.add(job);
			return projects;
		}
		
		public IntrootDepRes.IntrootDepResDescriptor getDefaultResult() {
			return Jenkins.getInstance().getDescriptorByType(IntrootDepResBuild.IntrootDepResBuildDescriptor.class);
		}
		
		public DescriptorExtensionList<IntrootDepRes, IntrootDepResDescriptor> getResults() {	
			return IntrootDepRes.getDescriptors();
		}
	}
}
