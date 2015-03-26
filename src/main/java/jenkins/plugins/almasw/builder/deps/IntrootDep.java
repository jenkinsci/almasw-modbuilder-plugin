package jenkins.plugins.almasw.builder.deps;

import java.io.File;
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
import hudson.model.Project;
import hudson.util.ListBoxModel;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.jelly.ThisTagLibrary;

public class IntrootDep extends AbstractDescribableImpl<IntrootDep> implements Serializable {

	public final String project;
	public final boolean isArtifact;
	public final String location;
	public final IntrootDepRes result;

	@DataBoundConstructor
	public IntrootDep(String project, boolean isArtifact, String location, String introot, IntrootDepRes result) {
		
		this.project = project;
		this.isArtifact = isArtifact;
		this.location = location;
		this.result = result;
	}

	@Exported
	public String getProject() {
		return project;
	}
	
	@Exported
	public boolean getIsArtifact() {
		return isArtifact;
	}

	@Exported
	public String getLocation() {
		return location;
	}
	
	public String getIntroot() {
		StringBuilder introot = new StringBuilder();
		
		try {
			if(this.getIsArtifact()) {
				String id = this.getResult().getJenkinsId();
				introot.append(this.getProjectRootArtifact(id));
			} else {
				introot.append(this.getProjectWorkspace());
			}
			introot.append(File.separator).append(this.getACSSW());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return introot.toString();
	}
	
	public String getACSSW() {
		if(this.getLocation() == null || this.getLocation().isEmpty()) {
			return "ALMASW-" + this.getProject() + File.separator + "ACSSW";
		} else {
			return this.getLocation();
		}
	}
	
	public StringBuilder getProjectWorkspace() {
		StringBuilder builder = new StringBuilder();
		builder.append(this.getProjectRoot());
		builder.append(File.separator);
		builder.append("workspace");
		return builder;
	}
	
	public StringBuilder getProjectRootArtifact(int id) {
		return this.getProjectRootArtifact(String.valueOf(id));
	}
		
	public StringBuilder getProjectRootArtifact(String type) {
		StringBuilder builder = new StringBuilder();
		builder.append(this.getBuildRootId(type));
		builder.append(File.separator);
		builder.append("archive");
		return builder;
	}
	
	public StringBuilder getBuildRootId(int id) {
		return this.getBuildRootId(String.valueOf(id));
	}
	
	public StringBuilder getBuildRootId(String id) {
		StringBuilder builder = new StringBuilder();
		builder.append(this.getProjectRootBuild());
		builder.append(File.separator);
		builder.append(id);
		return builder;
	}
	
	public StringBuilder getProjectRootBuild() {
		StringBuilder builder = new StringBuilder();
		builder.append(this.getProjectRoot());
		builder.append(File.separator);
		builder.append("builds");
		return builder;
	}
	
	public StringBuilder getProjectRoot() {
		StringBuilder builder = new StringBuilder();
		builder.append("$JENKINS_HOME");
		builder.append(File.separator);
		builder.append("jobs");
		builder.append(File.separator);
		builder.append(this.getProject());
		return builder;
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
			for (Project project : Jenkins.getInstance().getAllItems(Project.class))
				projects.add(project.getName());
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
