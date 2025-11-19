<script lang="ts">
	import Modal from '$lib/components/Modal.svelte';
	import { fade } from 'svelte/transition';
	import { invalidateAll } from '$app/navigation';

	let { data } = $props();

	let showAddDeviceModal = $state(false);
	let description = $state('');
	let step = $state<'form' | 'code'>('form');

	let code = $state('');
	let expiresAt = $state<Date | null>(null);
	let error = $state<string | null>(null);
	let loading = $state(false);

	let interval: ReturnType<typeof setInterval>;
	let countdown = $state('');

	function openAddDeviceModal() {
		description = '';
		step = 'form';
		code = '';
		expiresAt = null;
		error = null;
		loading = false;
		showAddDeviceModal = true;
		clearInterval(interval);
	}

	function closeAddDeviceModal() {
		showAddDeviceModal = false;
		invalidateAll();
	}

	async function generateCode() {
		loading = true;
		error = null;

		try {
			const res = await fetch('/api/devices/register/generate-code', {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ description })
			});

			if (!res.ok) {
				const { message } = await res.json();
				throw new Error(message || 'Failed to generate code');
			}

			const result = await res.json();
			code = result.code;
			expiresAt = new Date(result.expiresAt);
			step = 'code';
			startCountdown();
		} catch (e: any) {
			error = e.message;
		} finally {
			loading = false;
		}
	}

	function startCountdown() {
		if (!expiresAt) {
			countdown = 'Expired'; // Handle immediately if null
			return;
		}

		const update = () => {
			if (!expiresAt) {
				clearInterval(interval);
				return;
			}
			const diff = expiresAt.getTime() - Date.now();
			if (diff <= 0) {
				countdown = 'Expired';
				clearInterval(interval);
				return;
			}
			const minutes = Math.floor(diff / 1000 / 60);
			const seconds = Math.floor((diff / 1000) % 60);
			countdown = `${minutes}:${seconds.toString().padStart(2, '0')}`;
		};

		update();
		interval = setInterval(update, 1000);
	}
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
			onclick={openAddDeviceModal}
			class="px-4 py-2 bg-indigo-600 text-white text-sm font-medium rounded-lg hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 transition-colors"
		>
			Add Device
		</button>
	</div>

	<div class="bg-white shadow-sm rounded-xl border border-gray-200 overflow-hidden">
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
										class="shrink-0 h-10 w-10 bg-indigo-100 rounded-full flex items-center justify-center text-indigo-600"
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
										<div class="text-sm font-medium text-gray-900">
											{device.model}
										</div>
										<div class="text-sm text-gray-500">
											{device.description || device.id}
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
							<td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
								{device.registeredAt?.toLocaleString()}
							</td>
							<td class="px-6 py-4 whitespace-nowrap">
								<span
									class="px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800"
								>
									Active
								</span>
							</td>
							<td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
								<button class="text-indigo-600 hover:text-indigo-900">Edit</button>
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
				<h3 class="mt-2 text-sm font-medium text-gray-900">No devices</h3>
				<p class="mt-1 text-sm text-gray-500">Get started by registering a new device.</p>
			</div>
		{/if}
	</div>
</div>

<Modal show={showAddDeviceModal} onClose={closeAddDeviceModal}>
	{#if step === 'form'}
		<div transition:fade>
			<div class="p-6">
				<h2 class="text-xl font-bold text-gray-900">Register a New Device</h2>
				<p class="mt-1 text-sm text-gray-600">
					Provide a short description for the device, then generate a registration code to enter on
					the device itself.
				</p>

				<div class="mt-6">
					<label for="description" class="block text-sm font-medium text-gray-700">
						Description
					</label>
					<div class="mt-1">
						<input
							type="text"
							name="description"
							id="description"
							bind:value={description}
							class="block w-full shadow-sm sm:text-sm border-gray-300 rounded-md focus:ring-indigo-500 focus:border-indigo-500"
							placeholder="e.g., John's Test Phone"
						/>
					</div>
				</div>

				{#if error}
					<div class="mt-4 text-sm text-red-600">
						{error}
					</div>
				{/if}

				<div class="mt-6 flex justify-end gap-3">
					<button
						type="button"
						class="px-4 py-2 bg-white border border-gray-300 text-sm font-medium rounded-lg hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
						onclick={closeAddDeviceModal}
					>
						Cancel
					</button>
					<button
						type="button"
						disabled={!description || loading}
						class="px-4 py-2 bg-indigo-600 text-white text-sm font-medium rounded-lg hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50"
						onclick={generateCode}
					>
						{#if loading}
							Generating...
						{:else}
							Generate New Code
						{/if}
					</button>
				</div>
			</div>
		</div>
	{:else if step === 'code'}
		<div transition:fade class="p-6 text-center">
			<h2 class="text-xl font-bold text-gray-900">Enter Code on Device</h2>
			<p class="mt-1 text-sm text-gray-600">
				Enter the following code on your device to complete the registration.
			</p>

			<div
				class="my-8 bg-gray-100 rounded-lg p-4 inline-block tracking-[1.5rem] text-center ml-4"
			>
				<p class="text-5xl font-bold text-gray-900">{code}</p>
			</div>

			<p class="text-sm text-gray-500">
				Code expires in: <span class="font-medium text-gray-700">{countdown}</span>
			</p>

			<div class="mt-8 flex justify-end gap-3">
				<button
					type="button"
					class="px-4 py-2 bg-indigo-600 text-white text-sm font-medium rounded-lg hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
					onclick={closeAddDeviceModal}
				>
					Done
				</button>
			</div>
		</div>
	{/if}
</Modal>
