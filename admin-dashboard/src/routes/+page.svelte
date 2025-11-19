<script lang="ts">
    // In a real app, we'd fetch stats here
    const stats = [
        {
            name: "Total Devices",
            value: "4",
            change: "+4",
            changeType: "positive",
        },
        {
            name: "Active Sessions",
            value: "0",
            change: "0%",
            changeType: "neutral",
        },
        {
            name: "Policy Violations",
            value: "0",
            change: "0%",
            changeType: "positive",
        },
    ];
</script>

<div class="space-y-6">
    <div>
        <h1 class="text-2xl font-bold text-gray-900">Dashboard</h1>
        <p class="mt-1 text-sm text-gray-500">
            Overview of your device management system.
        </p>
    </div>

    <div class="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
        {#each stats as stat}
            <div
                class="bg-white overflow-hidden shadow-sm rounded-xl border border-gray-200"
            >
                <div class="p-5">
                    <div class="flex items-center">
                        <div class="flex-shrink-0">
                            <!-- Icon placeholder -->
                            <div
                                class="h-10 w-10 rounded-md bg-indigo-500 flex items-center justify-center text-white"
                            >
                                <svg
                                    class="h-6 w-6"
                                    fill="none"
                                    viewBox="0 0 24 24"
                                    stroke="currentColor"
                                >
                                    <path
                                        stroke-linecap="round"
                                        stroke-linejoin="round"
                                        stroke-width="2"
                                        d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"
                                    />
                                </svg>
                            </div>
                        </div>
                        <div class="ml-5 w-0 flex-1">
                            <dl>
                                <dt
                                    class="text-sm font-medium text-gray-500 truncate"
                                >
                                    {stat.name}
                                </dt>
                                <dd>
                                    <div
                                        class="text-lg font-medium text-gray-900"
                                    >
                                        {stat.value}
                                    </div>
                                </dd>
                            </dl>
                        </div>
                    </div>
                </div>
                <div class="bg-gray-50 px-5 py-3">
                    <div class="text-sm">
                        <a
                            href="/devices"
                            class="font-medium text-indigo-600 hover:text-indigo-900"
                            >View all</a
                        >
                    </div>
                </div>
            </div>
        {/each}
    </div>

    <div class="bg-white shadow-sm rounded-xl border border-gray-200 p-6">
        <h3 class="text-lg font-medium leading-6 text-gray-900">
            Recent Activity
        </h3>
        <div class="mt-4">
            <p class="text-gray-500 text-sm">No recent activity to display.</p>
        </div>
    </div>
</div>
