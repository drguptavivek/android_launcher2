<script lang="ts">
	import { fade } from 'svelte/transition';

	let { show, onClose, children } = $props();
</script>

{#if show}
	<div
		transition:fade={{ duration: 150 }}
		class="fixed inset-0 z-50 flex items-center justify-center bg-gray-900/80"
		onclick={onClose}
		onkeydown={(e) => e.key === 'Escape' && onClose()}
		role="dialog"
		aria-modal="true"
		aria-labelledby="modal-title"
		tabindex="-1"
	>
		<div
			class="relative bg-white rounded-2xl shadow-xl w-full max-w-md m-4"
			onclick={(e) => e.stopPropagation()}
			role="document"
		>
			{@render children?.()}
		</div>
	</div>
{/if}
