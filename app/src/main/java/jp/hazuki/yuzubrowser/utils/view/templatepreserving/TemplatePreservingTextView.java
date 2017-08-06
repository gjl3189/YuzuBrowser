/*
 * Copyright (C) 2017 Hazuki
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Copyright 2015 The Chromium Authors. All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//    * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
//    * Redistributions in binary form must reproduce the above
// copyright notice, this list of conditions and the following disclaimer
// in the documentation and/or other materials provided with the
// distribution.
//    * Neither the name of Google Inc. nor the names of its
// contributors may be used to endorse or promote products derived from
// this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package jp.hazuki.yuzubrowser.utils.view.templatepreserving;

import android.content.Context;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * A {@link AppCompatTextView} that truncates content within a template, instead of truncating
 * the template text. Truncation only happens if maxLines is set to 1 and there's not enough space
 * to display the entire content.
 * <p>
 * For example, given the following template and content
 * Template: "%s was closed"
 * Content: "https://www.google.com/webhp?sourceid=chrome-instant&q=potato"
 * <p>
 * the TemplatePreservingTextView would truncate the content but not the template text:
 * "https://www.google.com/webh... was closed"
 */
public class TemplatePreservingTextView extends AppCompatTextView {
    private String mTemplate;
    private CharSequence mContent = "";
    private CharSequence mVisibleText;

    /**
     * Builds an instance of an {@link TemplatePreservingTextView}.
     *
     * @param context A {@link Context} instance to build this {@link TextView} in.
     * @param attrs   An {@link AttributeSet} instance.
     */
    public TemplatePreservingTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Sets the template format string. setText() must be called after calling this method for the
     * new template text to take effect.
     *
     * @param template Template format string (e.g. "Closed %s"), or null. If null is passed, this
     *                 view acts like a normal TextView.
     */
    public void setTemplateText(String template) {
        mTemplate = TextUtils.isEmpty(template) ? null : template;
    }

    /**
     * This will take {@code text} and apply it to the internal template, building a new
     * {@link String} to set.  This {@code text} will be automatically truncated to fit within
     * the template as best as possible, making sure the template does not get clipped.
     */
    @Override
    public void setText(CharSequence text, BufferType type) {
        mContent = text != null ? text : "";
        setContentDescription(mTemplate == null ? mContent : String.format(mTemplate, mContent));
        updateVisibleText(0, true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int availWidth =
                MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        updateVisibleText(availWidth,
                MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private CharSequence getTruncatedText(int availWidth) {
        final TextPaint paint = getPaint();

        // Calculate the width the template takes.
        final String emptyTemplate = String.format(mTemplate, "");
        final float emptyTemplateWidth = paint.measureText(emptyTemplate);

        // Calculate the available width for the content.
        final float contentWidth = Math.max(availWidth - emptyTemplateWidth, 0.f);

        // Ellipsize the content to the available width.
        CharSequence clipped = TextUtils.ellipsize(mContent, paint, contentWidth, TruncateAt.END);

        // Build the full string, which should fit within availWidth.
        return String.format(mTemplate, clipped);
    }

    private void updateVisibleText(int availWidth, boolean unspecifiedWidth) {
        CharSequence visibleText;
        if (mTemplate == null) {
            visibleText = mContent;
        } else if (getMaxLines() != 1 || unspecifiedWidth) {
            visibleText = String.format(mTemplate, mContent);
        } else {
            visibleText = getTruncatedText(availWidth);
        }

        if (!visibleText.equals(mVisibleText)) {
            mVisibleText = visibleText;

            // BufferType.SPANNABLE is required so that TextView.getIterableTextForAccessibility()
            // doesn't call our custom setText(). See crbug.com/449311
            super.setText(mVisibleText, BufferType.SPANNABLE);
        }
    }
}
