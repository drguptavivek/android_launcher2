<script lang="ts">
    import type { PageData } from './$types';

    let { data } = $props();
</script>

<div class="space-y-6">
    <div class="flex items-center justify-between">
        <div>
            <h1 class="text-2xl font-bold text-gray-900">Telemetry</h1>
            <p class="mt-1 text-sm text-gray-500">Real-time device events and location data.</p>
        </div>
    </div>

    <div class="bg-white shadow-sm rounded-xl border border-gray-200 overflow-hidden">
        <div class="overflow-x-auto">
            <table class="min-w-full divide-y divide-gray-200">
                <thead class="bg-gray-50">
                    <tr>
                        <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Time</th>
                        <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Type</th>
                        <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">User / Device</th>
                        <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Data</th>
                    </tr>
                </thead>
                <tbody class="bg-white divide-y divide-gray-200">
                    {#each data.events as event}
                        <tr class="hover:bg-gray-50 transition-colors">
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                {event.createdAt?.toLocaleString()}
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap">
                                <span class="px-2.5 py-0.5 rounded-full text-xs font-medium 
                                    {event.type === 'LOGIN' ? 'bg-green-100 text-green-800' : 
                                     event.type === 'LOGOUT' ? 'bg-orange-100 text-orange-800' : 
                                     event.type === 'LOCATION' ? 'bg-blue-100 text-blue-800' : 
                                     'bg-gray-100 text-gray-800'}">
                                    {event.type}
                                </span>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap">
                                <div class="text-sm font-medium text-gray-900">{event.username || 'Unknown'}</div>
                                <div class="text-sm text-gray-500">{event.deviceId}</div>
                            </td>
                            <td class="px-6 py-4 text-sm text-gray-500 max-w-xs truncate">
                                {event.data}
                            </td>
                        </tr>
                    {/each}
                </tbody>
            </table>
        </div>
        {#if data.events.length === 0}
            <div class="text-center py-12">
                <p class="text-sm text-gray-500">No telemetry events found.</p>
            </div>
        {/if}
    </div>
</div>
