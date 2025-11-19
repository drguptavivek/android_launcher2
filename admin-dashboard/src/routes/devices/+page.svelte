<script lang="ts">
	let { data } = $props();
</script>

<div class="space-y-6">
	<div class="flex items-center justify-between">
		<div>
			<h1 class="text-2xl font-bold text-gray-900">Registered Devices</h1>
			<p class="mt-1 text-sm text-gray-500">
				Manage and monitor all connected Android devices.
			</p>
		</div>
		<button
			class="px-4 py-2 bg-indigo-600 text-white text-sm font-medium rounded-lg hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 transition-colors"
		>
			Add Device
		</button>
	</div>

	<div
		class="bg-white shadow-sm rounded-xl border border-gray-200 overflow-hidden"
	>
		<div class="overflow-x-auto">
			<table class="min-w-full divide-y divide-gray-200">
				<thead class="bg-gray-50">
					<tr>
						<th
							scope="col"
							class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
							>Device Info</th
						>
						<th
							scope="col"
							class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
							>Android Version</th
						>
						<th
							scope="col"
							class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
							>Registered</th
						>
						<th
							scope="col"
							class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
							>Status</th
						>
						<th scope="col" class="relative px-6 py-3">
							<span class="sr-only">Actions</span>
						</th>
					</tr>
				</thead>
				<tbody class="bg-white divide-y divide-gray-200">
					{#each data.devices as device}
						<tr class="hover:bg-gray-50 transition-colors">
							<td class="px-6 py-4 whitespace-nowrap">
								<div class="flex items-center">
									<div
										class="flex-shrink-0 h-10 w-10 bg-indigo-100 rounded-full flex items-center justify-center text-indigo-600"
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
												d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z"
											/>
										</svg>
									</div>
									<div class="ml-4">
										<div
											class="text-sm font-medium text-gray-900"
										>
											{device.model}
										</div>
										<div class="text-sm text-gray-500">
											{device.id}
										</div>
									</div>
								</div>
							</td>
							<td class="px-6 py-4 whitespace-nowrap">
								<span
									class="px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800"
								>
									Android {device.androidVersion}
								</span>
							</td>
							<td
								class="px-6 py-4 whitespace-nowrap text-sm text-gray-500"
							>
								{device.registeredAt?.toLocaleString()}
							</td>
							<td class="px-6 py-4 whitespace-nowrap">
								<span
									class="px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800"
								>
									Active
								</span>
							</td>
							<td
								class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium"
							>
								<a
									href="#"
									class="text-indigo-600 hover:text-indigo-900"
									>Edit</a
								>
							</td>
						</tr>
					{/each}
				</tbody>
			</table>
		</div>
		{#if data.devices.length === 0}
			<div class="text-center py-12">
				<svg
					class="mx-auto h-12 w-12 text-gray-400"
					fill="none"
					viewBox="0 0 24 24"
					stroke="currentColor"
				>
					<path
						stroke-linecap="round"
						stroke-linejoin="round"
						stroke-width="2"
						d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z"
					/>
				</svg>
				<h3 class="mt-2 text-sm font-medium text-gray-900">
					No devices
				</h3>
				<p class="mt-1 text-sm text-gray-500">
					Get started by registering a new device.
				</p>
			</div>
		{/if}
	</div>
</div>
