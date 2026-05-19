/**
 * UI Utilities (JavaScript Implementation)
 * Equivalent to Ui.kt
 * 
 * Unified UI operation encapsulation
 */

export class UIUtils {
  // Default duration for toast display (milliseconds)
  static TOAST_DELAY_DEFAULT = 3000;

  /**
   * Show toast message on page
   * @param {Object} message - Message object with content property
   */
  static showToast(message) {
    const content = message.content || '';
    
    if (content) {
      // Create wrapper div
      const wrapDiv = document.createElement('div');
      wrapDiv.classList.add('toast-wrapper');
      
      // Create content div
      const contentDiv = document.createElement('div');
      contentDiv.classList.add('toast-content');
      contentDiv.innerHTML = content;
      
      // Append content to wrapper
      wrapDiv.appendChild(contentDiv);
      
      // Add wrapper to body
      document.body.appendChild(wrapDiv);
      
      // Remove after timeout
      setTimeout(() => {
        if (document.body.contains(wrapDiv)) {
          document.body.removeChild(wrapDiv);
        }
      }, UIUtils.TOAST_DELAY_DEFAULT);
    }
  }
}
