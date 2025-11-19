<script lang="ts">
	let { data } = $props();
</script>

<h1>Registered Devices</h1>

<ul>
	{#each data.devices as device}
		<li>
			<strong>{device.model}</strong> ({device.id}) - Android {device.androidVersion}
			<br>
			<small>Registered: {device.registeredAt?.toLocaleString()}</small>
		</li>
	{/each}
</ul>
