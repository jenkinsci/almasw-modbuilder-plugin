package jenkins.plugins.almasw.builder;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;

import jenkins.plugins.almasw.builder.deps.IntrootDep;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

public class IntrootBuilder extends Builder {
	
	public static final String DEFAULT_PROFILE = "/alma/ACS-current/ACSSW/config/.acs/.bash_profile.acs";
	public static final String DEFAULT_ACS_INSTALL = "/alma/ACS-current";
	public static final String DEFAULT_ACS_INTROOT = "$ALMASW_ACSSW";
			
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
	private final boolean dry;
	private Date date;
	
	private transient AbstractBuild build;
	private transient Launcher launcher;
	private transient BuildListener listener;

	@DataBoundConstructor
	public IntrootBuilder(String acs, String module, boolean verbose,
			boolean pars, int jobs, int limit, boolean noStatic, boolean noIfr,
			List<IntrootDep> dependencies, boolean ccache, String introot, boolean dry) {

		this.cores = Runtime.getRuntime().availableProcessors();
		
		this.acs = acs;
		this.module = module;
		this.verbose = verbose;
		this.pars = pars;
		this.jobs = jobs;
		this.limit = limit;
		this.noStatic = noStatic;
		this.noIfr = noIfr;
		this.dependencies = dependencies;
		this.ccache = ccache;
		this.introot = introot;
		this.dry = dry;
	}
	
	@SuppressWarnings("deprecation")
	public Map<String, String> getBuildEnv() throws Exception {
		
		String number = String.valueOf(this.build.getNumber());
		String id = this.build.getId();
		String url = this.build.getUrl();
		String name = this.build.getProject().getName();
		String tag = "$JOB_NAME-$BUILD_NUMBER";
		
		StringBuilder workspace = new StringBuilder();
		workspace.append(this.getBuild().getProject().getRootDir().getCanonicalPath());
		workspace.append(File.separator);
		workspace.append(this.getBuild().getProject().getWorkspace().getName());
		
		String acs = this.getDescriptor().getAcsInstall();
		acs = acs == null || acs.isEmpty() ? DEFAULT_ACS_INSTALL : acs;
		
		String rtai = acs + File.separator + "rtai";
		String linux = acs + File.separator + "rtlinux";
		String profile =  acs + File.separator + "ACSSW/config/.acs/.bash_profile.acs";
		
		String almabtag = "ALMASW-$BUILD_TAG";
		String almasw = "$ALMASW_BTAG/ACSSW";
		String acsdata = "$ALMASW_BTAG/acsdata";
		String latest = "ALMASW-$JOB_NAME"; 
		String introot = almasw;
		 
		Map<String, String> envvars = new HashMap<String, String>();
		
		envvars.put("BUILD_NUMBER", number);
		envvars.put("BUILD_ID", id);
		envvars.put("BUILD_URL", url);
		envvars.put("JOB_NAME", name);
		envvars.put("BUILD_TAG", tag);
		envvars.put("WORKSPACE", workspace.toString());
		
		envvars.put("ALMASW_PROFILE", profile);
		envvars.put("RTAI_HOME", linux);
		envvars.put("LINUX_HOME", linux);
		
		envvars.put("ALMASW_BTAG", almabtag);
		envvars.put("ALMASW_LATEST", latest);
		
		envvars.put("ALMASW_ACSSW", almasw);
		envvars.put("ALMASW_INTROOT", introot);
		envvars.put("INTROOT", introot);
		envvars.put("ALMASW_ACSDATA", acsdata);
		envvars.put("ACSDATA", acsdata);
			
		if(this.verbose)
			envvars.put("MAKE_VERBOSE", "on");
		
		if(this.noIfr)
			envvars.put("MAKE_NOIFR_CHECK", "yes");
		
		if(this.noStatic) {
			envvars.put("_NOSTATIC", "yes");
			envvars.put("MAKE_NOSTATIC", "yes");
		}
		
		if(this.pars && this.cores > 1) {
			
			StringBuilder makePars = new StringBuilder();
			makePars.append("-j");
			
			if(this.jobs < 0) {
				makePars.append(this.cores - 1);
			} else if(this.jobs == 0){
				makePars.append(1);
			} else {
				makePars.append(this.jobs);
			}
			
			if( this.limit != 0 ) {
				makePars.append(" -l");
				if(this.limit < 0) {
					makePars.append(this.cores - 1);
				} else {
					makePars.append(this.limit);
				}
			}
			
			envvars.put("MAKE_PARS", makePars.toString());
		}
		
		if(this.ccache) {
			envvars.put("CCACHE_ROOT", this.getDescriptor().getCcacheInstall());
			envvars.put("CCACHE_DIR", ".ccache");
		}
		
		return envvars;
	}
	
	@Override
	public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
		
		this.build = build;
		this.launcher = launcher;
		this.listener = listener;
		
		PrintStream logger = listener.getLogger();
		FilePath workspace = build.getProject().getWorkspace();
		
		// environment setup
		
		Map<String, String> buildEnv =  build.getEnvVars();
		try {
			buildEnv.putAll(this.getBuildEnv());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// buid lifecycle
		
		ArrayList<String> commands = new ArrayList<String>();
		
		// setup phase
		commands.add("source $ALMASW_PROFILE");
		commands.add("getTemplateForDirectory INTROOT $ALMASW_INTROOT");
				
		// build phase
		
		// ccache setup, update path in order to cache builds
		if(this.ccache) {
			commands.add("export PATH=$CCACHE_ROOT/bin:$PATH");
			commands.add("mkdir -p $CCACHE_DIR");
		}
		
		commands.add("make build -C " + this.module);
		commands.add("ln -sf $ALMASW_BTAG $ALMASW_LATEST");
		commands.add("$PATH");
		
		try {
			for(String command: commands) {
				if(this.dry) {
					launcher.launch("echo " + command, buildEnv, logger, workspace).join();
				} else {
					launcher.launch(command, buildEnv, logger, workspace).join();
				}
			}
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	private AbstractBuild getBuild() throws Exception {
		if(build == null)
			throw new Exception("must be configured by the \"perform\" method");
		return build;
	}

	private Launcher getLauncher() throws Exception {
		if(launcher == null)
			throw new Exception("must be configured by the \"perform\" method");
		return launcher;
	}

	private BuildListener getListener() throws Exception {
		if(listener == null)
			throw new Exception("must be configured by the \"perform\" method");
		return listener;
	}
	
	@Exported
	public List<IntrootDep> getDependencies() {
		return dependencies;
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
	public boolean getDry() {
		return dry;
	}

	@Override
	public IntrootBuilderDescriptor getDescriptor() {
		return (IntrootBuilderDescriptor) super.getDescriptor();
	}
	
	@Extension
	public static final class IntrootBuilderDescriptor extends BuildStepDescriptor<Builder> {

		private String ccacheInstall;
		private String acsInstall;
		
		public IntrootBuilderDescriptor() {
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
			File almaInstall = new File(basePath);
			
			ListBoxModel items = new ListBoxModel();
			Set<String> installations = new HashSet<String>();
			
			items.add(basePath + File.separator + "ACS-current");
			
			try {
				for(File installation : almaInstall.listFiles(
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
