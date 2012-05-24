// details.js

(function($) {

$(document).ready(function(event) {
    if (!('open' in document.createElement('details'))) {
        $(document).bind("DOMNodeInserted", handle);
        handle(event);
    }
});

function select(node, selector) {
    var set = $(node).find(selector);
    if (node && $(node).is(selector)) {
        set = set.add(node);
    }
    return set;
}

function notSummary(){
    return this.tagName != 'summary' && this.tagName != 'SUMMARY';
}

function handle(event) {
    // Loop through all `details` elements
    select(event.target, "details").each(function() {

      // Store a reference to the current `details` element in a variable
      var $details = $(this),
          // Store a reference to the `summary` element of the current `details` element (if any) in a variable
          $detailsSummary = $('summary', $details),
          // Do the same for the info within the `details` element
          $detailsNotSummary = $details.children().filter(notSummary),
          // This will be used later to look for direct child text nodes
          $detailsNotSummaryContents = $details.contents().filter(notSummary);

    $detailsSummary.css("display", "list-item").css("margin-left", "1em").css("outline", "none");

      // If there is no `summary` in the current `details` element…
      if (!$detailsSummary.length) {
        // …create one with default text
        $detailsSummary = $(document.createElement('summary')).text('Details').prependTo($details);
      }

      // Look for direct child text nodes
      if ($detailsNotSummary.length !== $detailsNotSummaryContents.length) {
        // Wrap child text nodes in a `span` element
        $detailsNotSummaryContents.filter(function() {
          // Only keep the node in the collection if it’s a text node containing more than only whitespace
          return (this.nodeType === 3) && (/[^\t\n\r ]/.test(this.data));
        }).wrap('<span>');
        // There are now no direct child text nodes anymore — they’re wrapped in `span` elements
        $detailsNotSummary = $details.children().filter(notSummary);
      }

      // Hide content unless there’s an `open` attribute
      // Chrome 10 already recognizes the `open` attribute as a boolean (even though it doesn’t support rendering `<details>` yet
      // Other browsers without `<details>` support treat it as a string
      try {
        open = $details.attr('open');
      } catch (e) {}
      if (typeof open == 'string' || (typeof open == 'boolean' && open)) {
        $details.addClass('open');
        $detailsNotSummary.show();
      } else {
        $detailsNotSummary.hide();
      }

      // Set the `tabIndex` of the `summary` element to 0 to make it keyboard accessible
      $detailsSummary.attr('tabIndex', 0).click(function() {
        // Focus on the `summary` element
        $detailsSummary.focus();
        // Toggle the `open` attribute of the `details` element
        typeof $details.attr('open') !== 'undefined' ? $details.removeAttr('open') : $details.attr('open', 'open');
        // Toggle the additional information in the `details` element
        $detailsNotSummary.toggle(0);
        $details.toggleClass('open');
      }).keyup(function(event) {
        if (13 === event.keyCode || 32 === event.keyCode) {
          // Enter or Space is pressed — trigger the `click` event on the `summary` element
          // Opera already seems to trigger the `click` event when Enter is pressed
          if (!($.browser.opera && 13 === event.keyCode)) {
            event.preventDefault();
            $detailsSummary.click();
          }
        }
      });

    });

  }

})(window.jQuery);

