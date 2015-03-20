package jenkins.plugins.almasw.builder;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import jenkins.plugins.almasw.builder.deps.IntrootDep;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

import com.google.common.base.CaseFormat;
import com.thoughtworks.xstream.io.path.Path;

import hudson.Extension;
import hudson.Launcher;
import hudson.Proc;
import hudson.Launcher.ProcStarter;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;

public class IntrootBuilder extends Builder {
	
	public static final String DEFAULT_PROFILE = "/alma/ACS-current/ACSSW/config/.acs/.bash_profile.acs";

	public final int cores;

	private String acs;
	private final String module;
	private final boolean verbose;
	private final boolean pars;
	private final int jobs;
	private final int limit;
	private final boolean noStatic;
	private final boolean noIfr;
	private final List<IntrootDep> dependencies;
	private final boolean ccache;
	private final String introot;
	private Date date;
	
	// configured at perform execution
	AbstractBuild abstractBuild;
	Launcher launcher;
	BuildListener buildListener;

	@DataBoundConstructor
	public IntrootBuilder(String acs, String module, boolean verbose,
			boolean pars, int jobs, int limit, boolean noStatic, boolean noIfr,
			List<IntrootDep> dependencies, boolean ccache, String introot) {

		this.cores = Runtime.getRuntime().availableProcessors();
		
		this.acs = acs;
		this.module = module;
		this.verbose = verbose;
		this.pars = pars;
		this.jobs = jobs;
			//(jobs < 0 ) ? ((this.cores > 1) ? this.cores : 1) : ((jobs == 0) ? jobs : 1);
		this.limit = limit;
			//(limit < 0 ) ? ((this.cores > 1) ? this.cores : 1) : ((limit == 0) ? jobs : 1);
		this.noStatic = noStatic;
		this.noIfr = noIfr;
		this.dependencies = dependencies;
		this.ccache = ccache;
		this.introot = introot;
	}
	
	@Exported
	public String getModule() {
		return module;
	}

	@Exported
	public boolean getVerbose() {
		return verbose;
	}
	
	@Exported
	public boolean getPars() {
		return pars;
	}

	@Exported
	public int getCores() {
		return cores;
	}

	@Exported
	public int getJobs() {
		return jobs;
	}

	@Exported
	public int getLimit() {
		return limit;
	}

	@Exported
	public boolean getNoStatic() {
		return noStatic;
	}

	@Exported
	public boolean getNoIfr() {
		return noIfr;
	}

	@Exported
	public String getAcs() {
		return acs;
	}
	
	@Exported
	public boolean getCcache() {
		return ccache;
	}
	
	@Exported
	public String getIntroot() {
		return introot;
	}

	@Exported
	public List<IntrootDep> getDependencies() {
		return dependencies;
	}
	
	public void println(String message) {
		this.buildListener.getLogger().println(message);
	}
	
	public AbstractBuild getAbstractBuild() throws Exception {
		if(abstractBuild == null)
			throw new Exception("must be configured by the \"perform\" method");
		return abstractBuild;
	}

	public Launcher getLauncher() throws Exception {
		if(launcher == null)
			throw new Exception("must be configured by the \"perform\" method");
		return launcher;
	}

	public BuildListener getBuildListener() throws Exception {
		if(buildListener == null)
			throw new Exception("must be configured by the \"perform\" method");
		return buildListener;
	}

	public void printEnvironment() {
		this.println("");
		this.println("almasw modbuilder environment");
		this.println(this.date.toGMTString());
		this.println("");
		if(this.getDependencies() != null) {
			this.println("Dependencies");
			for(IntrootDep dependency: this.getDependencies())
				this.println("\t- " + dependency.toString());
		}
		this.println("");
	}
	
	public Map<String, String> envvars() {
		Map<String, String> envvars = new HashMap<String, String>();
		
		// general jenkins
		envvars.put("BUILD_NUMBER", String.valueOf(this.abstractBuild.getNumber()));
		envvars.put("BUILD_ID", this.abstractBuild.getId());
		envvars.put("BUILD_URL", this.abstractBuild.getUrl());
		//envvars.put("JOB_NAME", String.valueOf(this.abstractBuild.getExternalizableId().split("#")[0]));
		envvars.put("JOB_NAME", this.abstractBuild.getProject().getName());
		envvars.put("BUILD_TAG", "$JOB_NAME-$BUILD_NUMBER");
		
		try {
			//getBuildDir
			envvars.put("WORKSPACE", 
					this.abstractBuild.getProject().getRootDir().getCanonicalPath() + 
					File.separator + 
					this.abstractBuild.getProject().getWorkspace().getName() );
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// almasw build configuration
		envvars.put("RTAI_HOME",this.getDescriptor().getAcsInstall() + File.separator + "rtai");
		envvars.put("LINUX_HOME",this.getDescriptor().getAcsInstall() + File.separator + "rtlinux");
		envvars.put("BUILDER_PROFILE",this.getDescriptor().getAcsInstall() + File.separator + "ACSSW/config/.acs/.bash_profile.acs");
		envvars.put("ALMASW_ACSSW", "$WORKSPACE/ALMASW-$BUILD_TAG/ACSSW");
		envvars.put("ALMASW_ACSDATA", "$WORKSPACE/ALMASW-$BUILD_TAG/acsdata");
		envvars.put("INTROOT","$ALMASW_ACSSW");
		
		return envvars;
	}
	
	@Override
	public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
		
		this.abstractBuild = build;
		this.launcher = launcher;
		this.buildListener = listener;
		
		this.date = new Date();
		this.printEnvironment();
		
		Map<String, String> envvars = build.getEnvVars();
		envvars.putAll(this.envvars());
		
		try {
		
			Proc proc1 = launcher.launch(
					"echo $INTROOT", 
					envvars, 
					listener.getLogger(),
					build.getProject().getWorkspace());
			proc1.join();
			this.println("");
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		this.println("");
		
		return true;
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

		private String ccacheInstall;
		private String acsInstall;
		
		public DescriptorImpl() {
			load();
		}
		
		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return true;
		}
		
		public String getDisplayName() {
			return "ALMA Software Module Builder";
		}
		
		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			ccacheInstall = formData.getString("ccacheInstall");
			acsInstall = formData.getString("acsInstall");
			save();
			return super.configure(req, formData);
		}

		public FormValidation doCheckName(@QueryParameter String value) throws IOException, ServletException {
			if (value.length() == 0)
				return FormValidation.error("Please set a name");
			if (value.length() < 4)
				return FormValidation.warning("Isn't the name too short?");
			return FormValidation.ok();
		}
		
		public ListBoxModel doFillAcsItems() {
			
			String basePath = (this.acsInstall != null && !this.acsInstall.isEmpty()) ? this.acsInstall : "/alma"; 
			File almaBase = new File(basePath);
			
			ListBoxModel items = new ListBoxModel();
			Set<String> installations = new HashSet<String>();
			
			items.add(basePath + File.separator + "ACS-current");
			
			try {
				for(File installation : almaBase.listFiles(
						new FileFilter() {
							public boolean accept(File pathname) { 
								return pathname.isDirectory(); 
							}
				})) {
					if(installations.add(installation.getCanonicalPath()))
						items.add(installation.getCanonicalPath());
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}	
			
			installations.clear();
			return items;
		}

		public String getCcacheInstall() {
			return ccacheInstall;
		}
		
		public String getAcsInstall() {
			return acsInstall;
		}
	}
}
