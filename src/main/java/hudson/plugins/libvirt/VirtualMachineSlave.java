/**
 *  Copyright (C) 2010, Byte-Code srl <http://www.byte-code.com>
 *  Copyright (C) 2012  Philipp Bartsch <tastybug@tastybug.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Date: Mar 04, 2010
 * @author Marco Mornati<mmornati@byte-code.com>
 * @author Philipp Bartsch <tastybug@tastybug.com>
 */
package hudson.plugins.libvirt;

import hudson.AbortException;
import hudson.Extension;
import hudson.Util;
import hudson.model.TaskListener;
import hudson.plugins.libvirt.lib.VirtException;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Slave;
import hudson.slaves.Cloud;
import hudson.slaves.ComputerListener;
import hudson.slaves.NodeProperty;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.RetentionStrategy;
import hudson.slaves.SlaveComputer;
import hudson.util.ListBoxModel;

import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;

public class VirtualMachineSlave extends Slave {

    static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(VirtualMachineSlave.class.getName());

    private String      hypervisorDescription;
    private String      snapshotName;
    private String      virtualMachineName;
    private int         startupWaitingPeriodSeconds;
    private String      shutdownMethod;
    private boolean     rebootAfterRun;
    private int         startupTimesToRetryOnFailure;
    private String      beforeJobSnapshotName;


    @DataBoundConstructor
    public VirtualMachineSlave(String name, String nodeDescription, String remoteFS, String numExecutors,
            Mode mode, String labelString, VirtualMachineLauncher launcher, ComputerLauncher delegateLauncher,
            RetentionStrategy<? extends Computer> retentionStrategy, List<? extends NodeProperty<?>> nodeProperties,
            String hypervisorDescr, String vmName, String snapshot, int waitPeriod,
            String shutdown, boolean reboot, int retry, String preSnapshot)
            throws
            Descriptor.FormException, IOException {
        super(name, nodeDescription, remoteFS, Util.tryParseNumber(numExecutors, 1).intValue(), mode, labelString,
              launcher == null ? new VirtualMachineLauncher(delegateLauncher, hypervisorDescr,
                                                            vmName, snapshot, waitPeriod, retry) : launcher,
              retentionStrategy, nodeProperties);
        this.hypervisorDescription = hypervisorDescr;
        this.virtualMachineName = vmName;
        this.snapshotName = snapshot;
        this.startupWaitingPeriodSeconds = waitPeriod;
        this.shutdownMethod = shutdown;
        this.rebootAfterRun = reboot;
        this.startupTimesToRetryOnFailure = retry;
        this.beforeJobSnapshotName = preSnapshot;
    }

    public String getHypervisorDescription() {
        return hypervisorDescription;
    }

    public String getVirtualMachineName() {
        return virtualMachineName;
    }

    public String getSnapshotName() {
        return snapshotName;
    }

    public int getStartupWaitingPeriodSeconds() {
        return startupWaitingPeriodSeconds;
    }

    public String getShutdownMethod() {
        return shutdownMethod;
    }

    public boolean getRebootAfterRun() {
        return rebootAfterRun;
    }

    public int getStartupTimesToRetryOnFailure() {
        return startupTimesToRetryOnFailure;
    }

    public String getBeforeJobSnapshotName() {
        return beforeJobSnapshotName;
    }

    public ComputerLauncher getDelegateLauncher() {
        return ((VirtualMachineLauncher) getLauncher()).getDelegate();
    }

    @Override
    public Computer createComputer() {
        return new VirtualMachineSlaveComputer(this);
    }

    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    @Extension
    public static class VirtualMachineComputerListener extends ComputerListener {

        @Override
        public void preLaunch(Computer c, TaskListener taskListener) throws IOException, InterruptedException {
            /* We may be called on any slave type so check that we should
             * be in here. */
            if (!(c.getNode() instanceof VirtualMachineSlave)) {
                return;
            }

            VirtualMachineLauncher vmL = (VirtualMachineLauncher) ((SlaveComputer) c).getLauncher();
            try {
                Hypervisor vmC = vmL.findOurHypervisorInstance();
                if (!vmC.markVMOnline(c.getDisplayName(), vmL.getVirtualMachineName())) {
                    throw new AbortException("Capacity threshold  (" + vmC.getMaxOnlineSlaves() + ") reached at hypervisor \"" + vmC.getHypervisorDescription() + "\", slave commissioning delayed.");
                }
            } catch (VirtException e) {
                LOGGER.log(Level.WARNING, "aborting slave launch due to:", e);
                throw new AbortException(e.getMessage());
            }
        }
    }

    @Extension
    public static final class DescriptorImpl extends SlaveDescriptor {

        private String hypervisorDescription;
        private String virtualMachineName;
        private String snapshotName;

        public DescriptorImpl() {
            load();
        }

        public String getDisplayName() {
            return "Slave virtual computer running on a virtualization platform (via libvirt)";
        }

        @Override
        public boolean isInstantiable() {
            return true;
        }

        public List<VirtualMachine> getDefinedVirtualMachines(String description) {
            List<VirtualMachine> virtualMachinesList = new ArrayList<VirtualMachine>();
            Hypervisor hypervisor = getHypervisorByDescription(description);
            if (hypervisor != null) {
                virtualMachinesList.addAll(hypervisor.getVirtualMachines());
        }
            Collections.sort(virtualMachinesList);
            return virtualMachinesList;
        }

        public String[] getDefinedSnapshots(String description, String vmName) {
            Hypervisor hypervisor = getHypervisorByDescription(description);
            if (hypervisor != null) {
                String[] snapS = hypervisor.getSnapshots(vmName);
                return snapS;
            }
            return new String[0];
        }

        public ListBoxModel doFillHypervisorDescriptionItems() {
            ListBoxModel items = new ListBoxModel();
            for (Cloud cloud : Jenkins.getInstance().clouds) {
                if (cloud instanceof Hypervisor) {
                    items.add(((Hypervisor) cloud).getHypervisorURI(), ((Hypervisor) cloud).getHypervisorDescription());
                }
            }
            return items;
        }

        public String getHypervisorDescription() {
            return hypervisorDescription;
        }

        public String getVirtualMachineName() {
            return virtualMachineName;
        }

        public String getSnapshotName() {
            return snapshotName;
        }

        private Hypervisor getHypervisorByDescription(String description) {
            if (description != null && !description.equals("")) {
                for (Cloud cloud : Jenkins.getInstance().clouds) {
                    if (cloud instanceof Hypervisor && ((Hypervisor) cloud).getHypervisorDescription().equals(description)) {
                        return (Hypervisor) cloud;
                    }
                }
            }
            return null;
        }
    }
}
