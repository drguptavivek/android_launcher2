
<script lang="ts">
    import { enhance } from '$app/forms';
    import { invalidateAll } from '$app/navigation';

    export let data;

    let newPolicyName = '';
    let newPolicyConfig = JSON.stringify({
        allowedApps: ['com.android.settings', 'com.example.launcher'],
        systemToggles: {
            wifi: true,
            bluetooth: false
        }
    }, null, 2);

    async function createPolicy() {
        try {
            const response = await fetch('/api/policies', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    name: newPolicyName,
                    config: JSON.parse(newPolicyConfig)
                })
            });

            if (response.ok) {
                newPolicyName = '';
                invalidateAll();
            } else {
                alert('Failed to create policy');
            }
        } catch (e) {
            alert('Invalid JSON config');
        }
    }
</script>

<div class="p-6">
    <h1 class="text-2xl font-bold mb-6">Policy Management</h1>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
        <!-- Create Policy Form -->
        <div class="bg-white p-6 rounded-lg shadow-md">
            <h2 class="text-xl font-semibold mb-4">Create New Policy</h2>
            <div class="space-y-4">
                <div>
                    <label class="block text-sm font-medium text-gray-700">Policy Name</label>
                    <input
                        type="text"
                        bind:value={newPolicyName}
                        class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm p-2 border"
                        placeholder="e.g., School Mode"
                    />
                </div>
                <div>
                    <label class="block text-sm font-medium text-gray-700">Configuration (JSON)</label>
                    <textarea
                        bind:value={newPolicyConfig}
                        rows="10"
                        class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm p-2 border font-mono"
                    ></textarea>
                </div>
                <button
                    on:click={createPolicy}
                    class="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
                >
                    Create Policy
                </button>
            </div>
        </div>

        <!-- Policy List -->
        <div class="bg-white p-6 rounded-lg shadow-md">
            <h2 class="text-xl font-semibold mb-4">Existing Policies</h2>
            <div class="space-y-4">
                {#each data.policies as policy}
                    <div class="border rounded-md p-4 hover:bg-gray-50">
                        <div class="flex justify-between items-start">
                            <div>
                                <h3 class="font-medium text-lg">{policy.name}</h3>
                                <p class="text-xs text-gray-500">Created: {new Date(policy.createdAt).toLocaleString()}</p>
                            </div>
                            <span class="bg-gray-100 text-gray-800 text-xs font-medium px-2.5 py-0.5 rounded font-mono">
                                {policy.id.slice(0, 8)}...
                            </span>
                        </div>
                        <pre class="mt-2 text-xs bg-gray-100 p-2 rounded overflow-x-auto">
{JSON.stringify(JSON.parse(policy.config), null, 2)}
                        </pre>
                    </div>
                {/each}
                {#if data.policies.length === 0}
                    <p class="text-gray-500 text-center py-4">No policies found.</p>
                {/if}
            </div>
        </div>
    </div>
</div>
