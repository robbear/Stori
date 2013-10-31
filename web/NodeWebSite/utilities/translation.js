var locale = require('locale'),
    english = require('../languages/en'),
    french = require('../languages/fr'),
    japanese = require('../languages/ja');

exports.getStringsForLanguage = function(req) {
    var locales = new locale.Locales(req.headers["accept-language"]);
    var lang = locales.best();

    if (lang) {
        switch (lang.language) {
            case "en":
                return english.Strings();
            case "fr":
                return french.Strings();
            case "ja":
                return japanese.Strings();
        }
    }

    return english.Strings();
};
