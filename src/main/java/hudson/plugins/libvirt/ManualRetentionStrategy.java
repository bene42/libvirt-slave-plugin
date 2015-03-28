/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hudson.plugins.libvirt;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.slaves.RetentionStrategy;
import hudson.slaves.SlaveComputer;

public class ManualRetentionStrategy extends RetentionStrategy<SlaveComputer> {

    @DataBoundConstructor
    public ManualRetentionStrategy() {
    }

    public long check(SlaveComputer c) {
        if (c.isOffline() && !c.isConnecting() && c.isLaunchSupported()
            && !(c.getOfflineCause() instanceof RevertOfflineCause)) {
           c.tryReconnect();
        }
        return 1;
    }

    @Override
    public void start(SlaveComputer c) {
        c.connect(false);
    }

    @Extension(ordinal=100)
    public static class DescriptorImpl extends Descriptor<RetentionStrategy<?>> {
        public String getDisplayName() {
            return "Manage this slave manually";
        }
    }

}
