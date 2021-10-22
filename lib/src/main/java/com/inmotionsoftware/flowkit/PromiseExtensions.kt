// MIT License
//
// Permission is hereby granted, free of charge, to any person obtaining a 
// copy of this software and associated documentation files (the "Software"), 
// to deal in the Software without restriction, including without limitation 
// the rights to use, copy, modify, merge, publish, distribute, sublicense, 
// and/or sell copies of the Software, and to permit persons to whom the 
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
// IN THE SOFTWARE.
//
//  PromiseExtension.kt
//  FlowKit
//
//  Created by Brian Howard
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//

package com.inmotionsoftware.flowkit

import com.inmotionsoftware.flowkit.FlowError
import com.inmotionsoftware.promisekt.*

fun <T> Promise<T>.back(closure: () -> T ): Promise<T> =
    canceled {
        when (it) {
            is FlowError.Back -> closure()
            is FlowError.Canceled -> throw it
        }
    }


fun <T> Promise<T>.cancel(closure: () -> T ): Promise<T> =
    canceled {
        when (it) {
            is FlowError.Back -> throw it
            is FlowError.Canceled -> closure()
        }
    }

fun <T> Promise<T>.canceled(closure: (FlowError) -> T ): Promise<T> =
    this.recover {
        if (it is FlowError) {
            Promise.value(closure(it))
        } else {
            throw it
        }
    }

fun <T> Resolver<T>.resolve(result: Result<T>) {
    when (result) {
        is Result.Success -> fulfill(result.value)
        is Result.Failure -> reject(result.cause)
    }
}