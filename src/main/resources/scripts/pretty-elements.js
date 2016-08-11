// relies on
// <script src="https://raw.githubusercontent.com/vkiryukhin/vkBeautify/master/vkbeautify.js"></script>
//
// makes all elements with class="pretty" pretty

document.addEventListener('DOMContentLoaded', function () {
    var all = document.getElementsByClassName("pretty");
    for(i=0; i<all.length; ++i) {
        var text = all[i].textContent;
        console.log("text = " + text);
        if ( text && text.startsWith("<") ) {
            text = vkbeautify.xml(text,1);
            all[i].textContent = text;
            console.log("pretty xml: " + text);
        }
        else if ( text && text.startsWith("{") ) {
            text = vkbeautify.json(text,1);
            all[i].textContent = text;
            console.log("pretty json: " + text );
        }
    }
});