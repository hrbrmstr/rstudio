/*
 * completion.ts
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

import { Plugin, PluginKey } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';

import React from 'react';
import ReactDOM from 'react-dom';

import { CompletionHandler, CompletionResult } from '../../api/completion';
import { EditorEvents, EditorEvent } from '../../api/events';
import { applyStyles } from '../../api/css';

import { CompletionPopup } from './completion-popup';

export function completionExtension(handlers: readonly CompletionHandler[], events: EditorEvents) {
  return {
    plugins: () => [new CompletionPlugin(handlers, events)]
  };
}

const key = new PluginKey('completion');

// TODO: Consolidate scroll / dismiss handling with insert symbol? (e.g. dismissable popup)

class CompletionPlugin extends Plugin {
  private readonly scrollUnsubscribe: VoidFunction;
  private readonly completionPopup: HTMLElement;

  constructor(handlers: readonly CompletionHandler[], events: EditorEvents) {
    super({
      key,
      view: () => ({
        update: (view: EditorView) => {
          for (const handler of handlers) {
            const result = handler.completions(view.state, 20);
            if (result) {
              this.showCompletions(view, handler, result);
              return;
            }
          }
          this.hideCompletions();
        },
        destroy: () => {
          this.scrollUnsubscribe();
          window.document.removeEventListener('focusin', this.focusChanged);

          ReactDOM.unmountComponentAtNode(this.completionPopup);
          this.completionPopup.remove();
        },
      }),
    });
    this.hideCompletions = this.hideCompletions.bind(this);
    this.scrollUnsubscribe = events.subscribe(EditorEvent.Scroll, this.hideCompletions);

    this.focusChanged = this.focusChanged.bind(this);
    window.document.addEventListener('focusin', this.focusChanged);

    this.completionPopup = window.document.createElement('div');
    this.completionPopup.tabIndex = 0;
    this.completionPopup.style.position = 'absolute';
    this.completionPopup.style.zIndex = '1000';
    window.document.body.appendChild(this.completionPopup);
    this.hideCompletions();
  }

  private showCompletions(view: EditorView, handler: CompletionHandler, result: CompletionResult) {

    // helper function to show the popup at the specified position
    const showPopup = (completions: any[]) => {
      
      const positionStyles = panelPositionStylesForPosition(view, result.pos, 200, 200);
      applyStyles(this.completionPopup, [], positionStyles);
      
      this.completionPopup.style.display = '';     
      ReactDOM.render(
        <CompletionPopup 
        completions={completions} 
        completionView={handler.completionView} />,
        this.completionPopup,
      );
    };
    
    // show completions (resolve promise if necessary)
    if (result.items instanceof Promise) {
      result.items.then(showPopup);
    } else {
      showPopup(result.items);
    }
  }

  private hideCompletions() {
    this.completionPopup.style.display = 'none';
  }

  private focusChanged() {
    if (
      window.document.activeElement !== this.completionPopup &&
      !this.completionPopup.contains(window.document.activeElement)
    ) {
      this.hideCompletions();
    }
  }
}
const kMinimumPanelPaddingToEdgeOfView = 5;
function panelPositionStylesForPosition(view: EditorView, pos: number, height: number, width: number) {
  const editorRect = view.dom.getBoundingClientRect();

  const selectionCoords = view.coordsAtPos(pos);

  const maximumTopPosition = Math.min(
    selectionCoords.bottom,
    window.innerHeight - height - kMinimumPanelPaddingToEdgeOfView,
  );
  const minimumTopPosition = editorRect.y;
  const popupTopPosition = Math.max(minimumTopPosition, maximumTopPosition);

  const maximumLeftPosition = Math.min(
    selectionCoords.right,
    window.innerWidth - width - kMinimumPanelPaddingToEdgeOfView,
  );
  const minimumLeftPosition = editorRect.x;
  const popupLeftPosition = Math.max(minimumLeftPosition, maximumLeftPosition);

  // styles we'll return
  const styles = {
    top: popupTopPosition + 'px',
    left: popupLeftPosition + 'px',
  };

  return styles;
}