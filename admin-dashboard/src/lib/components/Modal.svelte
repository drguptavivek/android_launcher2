<script lang="ts">
	import { fade } from 'svelte/transition';

	let { show, onClose } = $props();
</script>

{#if show}
	<div
		transition:fade={{ duration: 150 }}
		class="fixed inset-0 z-50 flex items-center justify-center bg-gray-900/80"
	>
		<div
			class="relative bg-white rounded-2xl shadow-xl w-full max-w-md m-4"
			onclick={(e) => e.stopPropagation()}
			role="document"
		>
			<!-- TODO: Using <slot> is deprecated. Use {@render ...} tags instead when fully adopting Svelte 5 render props -->
			<slot />
		</div>
	</div>
{/if}
