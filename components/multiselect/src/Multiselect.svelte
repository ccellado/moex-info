<script lang="ts">
  import Select from "svelte-select";

  class Item {
    value: string;
    label: string;
    group: string;
  }

  const searchSelectionItems = [
    { value: "secid", label: "secid", group: "▲ Ascending" },
    { value: "regnumber", label: "regnumber", group: "▲ Ascending" },
    { value: "name", label: "name", group: "▲ Ascending" },
    { value: "emitent_title", label: "emitent_title", group: "▲ Ascending" },
    { value: "tradedate", label: "tradedate", group: "▲ Ascending" },
    { value: "numtrades", label: "numtrades", group: "▲ Ascending" },
    { value: "open", label: "open", group: "▲ Ascending" },
    { value: "close", label: "close", group: "▲ Ascending" },
    { value: "secid", label: "secid", group: "▼ Descending" },
    { value: "regnumber", label: "regnumber", group: "▼ Descending" },
    { value: "name", label: "name", group: "▼ Descending" },
    { value: "emitent_title", label: "emitent_title", group: "▼ Descending" },
    { value: "tradedate", label: "tradedate", group: "▼ Descending" },
    { value: "numtrades", label: "numtrades", group: "▼ Descending" },
    { value: "open", label: "open", group: "▼ Descending" },
    { value: "close", label: "close", group: "▼ Descending" },
  ];

  const groupBy = (item) => item.group;
  const getSelectionLabel = (option) => (option.group.substring(0, 1) + " " + option.label);
  const inputAttrs = {};
  let selectedValue = undefined;

  function select(json: Array<Item>) {
    let str: Array<string> = [];
    for (let obj of json) {
      if (obj.group === "▼ Descending") {
        str.push("d_" + obj.value);
      } else {
        str.push(obj.value);
      }
    }
    return str;
  }
</script>

<style>
  :global(input) {
    margin: 0;
  }
  .themed {
    --multiItemActiveBG: #1abc9c;
    --multiItemBG: #111;
    --itemColor: #ddd;
    --inputColor: #ddd;
    --background: #333;
    --listBackground: #333;
    --listBorderRadius: 0px;
    --listEmptyPadding: 0px;
    --itemHoverBG: #444;
    --placeholderColor: #ddd;
    --borderRadius: 0px;
    --inputFontSize: 1rem;
    --groupTitleFontSize: 1rem;
    --multiItemMargin: .1em;
    --height: 2.5em;
    --border: 1px solid #000;
    --borderHoverColor: #aaa;
    --borderFocusColor: #1abc9c;
    --multiItemBorderRadius: 0px;
    --multiSelectPadding: 0px 0.2em;
  }
</style>

  {#if selectedValue!=undefined}
  <dl id="by_field">
    <input
      class="is-hidden"
      type="text"
      id="by"
      name="by"
      value={select(selectedValue)}
      />
  </dl>
  {:else}
  <dl id="by_field">
    <input
      class="is-hidden"
      type="text"
      id="by"
      name="by"
      value="secid"
      />
  </dl>
  {/if}
    <div class="themed">
      <Select
        items={searchSelectionItems}
        bind:selectedValue
        isMulti="true"
        {groupBy}
        placeholder=" Сортировать (чувствительно к порядку)..."
        {getSelectionLabel}
        inputAttributes={inputAttrs}
        />
  </div>