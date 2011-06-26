<p>
    Please enter a few bits of information so that a new extension can be created:
</p>
<div id="wizard-errors"></div>
<form action="#" method="post" class="aui">
    <div class="field-group">
        <label for="wizard-key" class="form-icon icon-required">Key</label>
        <input class="text" type="text" id="wizard-key" name="wizard-key" title="Extension key">
        <div class="description">A short identifier for your extension only containing numbers, letters, or the '-' or '_' keys.</div>
    </div>
    <div class="field-group">
        <label for="wizard-name" class="form-icon icon-required">Name</label>
        <input class="text" type="text" id="wizard-name" name="wizard-name" title="Extension name">
        <div class="description">A short name for your extension.</div>
    </div>
    <div class="field-group">
        <label for="wizard-description" class="form-icon icon-required">Description</label>
        <textarea id="wizard-description" name="wizard-description" title="Extension description" rows="5" cols="30"/>
        <div class="description">A short description for your extension.</div>
    </div>
    <div class="buttons-container">
        <div class="buttons">
            <input id="extension-wizard-create" class="button submit" type="submit" value="submit">
            <a id="extension-wizard-cancel" class="cancel" href="javascript:void(0)">Cancel</a>
        </div>
    </div>
</form>