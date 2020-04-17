/**
 *  Copyright (C) 2010, Byte-Code srl <http://www.byte-code.com>
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
 */

package hudson.plugins.libvirt;

import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;

public class VirtualMachine implements Serializable, Comparable<VirtualMachine> {

    private final String vmName;
    private final Hypervisor hypervisor;

    @DataBoundConstructor
    public VirtualMachine(Hypervisor h, String name) {
        this.hypervisor = h;
        this.vmName = name;
    }

    public String getVmName() {
        return vmName;
    }

    public Hypervisor getHypervisor() {
        return hypervisor;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VirtualMachine)) {
            return false;
        }

        VirtualMachine that = (VirtualMachine) o;

        if (hypervisor != null ? !hypervisor.equals(that.hypervisor) : that.hypervisor != null) {
            return false;
        }
        if (vmName != null ? !vmName.equals(that.vmName) : that.vmName != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = vmName != null ? vmName.hashCode() : 0;
        result = 31 * result + (hypervisor != null ? hypervisor.hashCode() : 0);
        return result;
    }

    public String getDisplayName() {
        return vmName + "@" + hypervisor.getHypervisorHost();
    }

    public int compareTo(VirtualMachine o) {
        return vmName.compareTo(o.getVmName());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("VirtualComputer");
        sb.append("{name='").append(vmName).append('\'');
        sb.append(", hypervisor=").append(hypervisor);
        sb.append('}');
        return sb.toString();
    }
}
